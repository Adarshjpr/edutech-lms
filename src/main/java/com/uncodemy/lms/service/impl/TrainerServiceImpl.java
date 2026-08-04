package com.uncodemy.lms.service.impl;

import com.uncodemy.lms.dto.request.TrainerCreateRequest;
import com.uncodemy.lms.dto.response.TrainerCreateResponse;
import com.uncodemy.lms.dto.response.TrainerResponse;
import com.uncodemy.lms.exception.DuplicateResourceException;
import com.uncodemy.lms.exception.ResourceNotFoundException;
import com.uncodemy.lms.model.Trainer;
import com.uncodemy.lms.model.enums.TrainerRole;
import com.uncodemy.lms.repository.TrainerRepository;
import com.uncodemy.lms.service.rule.EmailService;
import com.uncodemy.lms.service.rule.TrainerService;
import com.uncodemy.lms.util.IdGeneratorUtil;
import com.uncodemy.lms.util.PasswordGeneratorUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

/**
 * ============================================================================
 * TrainerServiceImpl   ---  API 1 + API 2 ka asli logic
 * ============================================================================
 *
 * @RequiredArgsConstructor  (Lombok)
 * ---------------------------------------------------------------------------
 * Saare "final" fields ka constructor khud bana deta hai.
 *
 * Matlab ye likhne ki zarurat nahi:
 *
 *     public TrainerServiceImpl(TrainerRepository r, PasswordEncoder p, ...) {
 *         this.trainerRepository = r;  ...
 *     }
 *
 * @Autowired FIELD PE KYUN NAHI LAGAYA?
 * ---------------------------------------------------------------------------
 * Constructor injection field injection se behtar hai:
 *
 *   ✔ Fields "final" ban sakti hain (koi galti se badal nahi sakta)
 *   ✔ Test me manually object bana sakte ho, Spring ki zarurat nahi
 *   ✔ Koi dependency missing ho to APP START HOTE HI pata chal jata hai,
 *     na ki request aane par
 *
 * @Transactional (class level)
 * ---------------------------------------------------------------------------
 * Har method ek DATABASE TRANSACTION me chalegi.
 *
 * Matlab: method ke beech me exception aa gayi to jitna
 * DB me likha tha wo SAB WAPAS HAT JAYEGA (rollback).
 *
 * Aadha-adhura data kabhi save nahi hoga.
 * ============================================================================
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class TrainerServiceImpl implements TrainerService {

    private final TrainerRepository trainerRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    /*
     * NOTE : BatchRepository yahan inject NAHI kiya.
     *
     * Kyunki wo class abhi bani hi nahi hai (Phase 3 me aayegi).
     * Isliye "totalBatches" abhi null jayega.
     *
     * Phase 3 me batch banne ke baad yahan
     * batchRepository add karke count bhar denge.
     */


    // ========================================================================
    // API 1 + 2  ---  CREATE TRAINER
    // ========================================================================
    @Override
    public TrainerCreateResponse createTrainer(TrainerCreateRequest request) {

        // --------------------------------------------------------------
        // STEP 1 : Input saaf karo
        // --------------------------------------------------------------
        /*
         * Admin ne " Rahul.Sharma " type kiya ho to bhi
         * DB me "rahul.sharma" hi jayega.
         *
         * Ye zaroori hai — warna existsByUsername("rahul.sharma")
         * false dega aur duplicate ban jayega.
         *
         * Locale.ROOT kyun?
         * -----------------------------------------------------------
         * Turkish locale me "I".toLowerCase() se "ı" (bina dot ka i)
         * banta hai — normal "i" nahi. Server ki language setting
         * badalne se code ka behaviour badal jata hai.
         *
         * Locale.ROOT lagane se har jagah same result aata hai.
         */
        String username = request.getUsername().trim().toLowerCase(Locale.ROOT);
        String email    = request.getEmail().trim().toLowerCase(Locale.ROOT);
        String name     = request.getName().trim();

        log.info("Trainer create request | username={} | email={}", username, email);

        // --------------------------------------------------------------
        // STEP 2 : Duplicate check
        // --------------------------------------------------------------
        /*
         * Ye check karne se user ko SAAF message milta hai:
         *   "Trainer already exists with username : 'rahul.sharma'"
         *
         * Bina iske DB ki unique constraint tootti aur
         * DataIntegrityViolationException aati — jiska message
         * technical hota hai aur user ko samajh nahi aata.
         *
         * NOTE: ye 100% guarantee nahi hai (race condition).
         * DB ki constraint hi final safety hai — wo GlobalExceptionHandler
         * ke Handler #8 se 409 ban jayegi.
         */
        if (trainerRepository.existsByUsername(username)) {
            throw new DuplicateResourceException("Trainer", "username", username);
        }

        if (trainerRepository.existsByEmail(email)) {
            throw new DuplicateResourceException("Trainer", "email", email);
        }

        // --------------------------------------------------------------
        // STEP 3 : Trainer ID banao  (TR101, TR102...)
        // --------------------------------------------------------------
        String lastTrainerId = trainerRepository.findLastTrainerId().orElse(null);
        String trainerId     = IdGeneratorUtil.nextTrainerId(lastTrainerId);

        log.debug("Generated trainerId={} (last was {})", trainerId, lastTrainerId);

        // --------------------------------------------------------------
        // STEP 4 : Random password banao
        // --------------------------------------------------------------
        /*
         * plainPassword ek hi baar banega aur do jagah jayega:
         *   1. Mail me  (trainer ko)
         *   2. Response me (admin ko, backup ke liye)
         *
         * DB me kabhi nahi. Method khatam hote hi ye variable
         * memory se gayab.
         */
        String plainPassword = PasswordGeneratorUtil.generate();

        // --------------------------------------------------------------
        // STEP 5 : Entity banao
        // --------------------------------------------------------------
        Trainer trainer = Trainer.builder()
                .trainerId(trainerId)
                .name(name)
                .username(username)
                .email(email)
                .designation(trim(request.getDesignation()))
                .phone(trim(request.getPhone()))
                .role(request.getRole())

                // DB me sirf HASH jayega, plain kabhi nahi
                .password(passwordEncoder.encode(plainPassword))

                .firstLogin(true)   // pehli login pe password change karna hoga
                .active(true)
                .build();

        // --------------------------------------------------------------
        // STEP 6 : Save
        // --------------------------------------------------------------
        Trainer saved = trainerRepository.save(trainer);

        log.info("Trainer created | id={} | trainerId={} | username={}",
                saved.getId(), saved.getTrainerId(), saved.getUsername());

        // --------------------------------------------------------------
        // STEP 7 : Mail bhejo  (API 2)
        // --------------------------------------------------------------
        /*
         * Ye line TURANT return kar degi.
         *
         * EmailService pe @Async laga hai — mail background
         * thread me jayegi. Admin ko response 200ms me mil jayega,
         * 2 second me nahi.
         *
         * Mail fail ho gayi to?
         * -----------------------------------------------------------
         * Trainer phir bhi bana rahega. Exception upar nahi aayegi
         * (EmailServiceImpl me try-catch hai).
         *
         * Yahi chahiye tha — mail server down hone se
         * trainer creation nahi rukni chahiye. Admin
         * response se password copy karke WhatsApp kar dega.
         */
        emailService.sendTrainerCredentials(
                saved.getEmail(),
                saved.getName(),
                saved.getUsername(),
                plainPassword
        );

        // --------------------------------------------------------------
        // STEP 8 : Response
        // --------------------------------------------------------------
        return TrainerCreateResponse.builder()
                .trainer(TrainerResponse.from(saved, 0))   // naya trainer = 0 batch
                .temporaryPassword(plainPassword)
                .mailSentTo(saved.getEmail())
                .build();
    }


    // ========================================================================
    // USERNAME AVAILABLE HAI YA NAHI
    // ========================================================================
    @Override
    @Transactional(readOnly = true)
    public boolean isUsernameAvailable(String username) {

        if (username == null || username.isBlank()) {
            return false;
        }

        String clean = username.trim().toLowerCase(Locale.ROOT);

        // "hai" ka ulta = "available"
        return !trainerRepository.existsByUsername(clean);
    }


    // ========================================================================
    // READ  ---  ek trainer
    // ========================================================================
    /**
     * @Transactional(readOnly = true)
     * -----------------------------------------------------------------------
     * Ye Hibernate ko batata hai ki "kuch likhna nahi hai".
     *
     * Fayda: Hibernate "dirty checking" skip kar deta hai —
     * matlab har object ki copy rakh ke compare nahi karta
     * ki kuch badla to nahi.
     *
     * Read APIs thodi fast ho jati hain.
     */
    @Override
    @Transactional(readOnly = true)
    public TrainerResponse getByTrainerId(String trainerId) {

        Trainer trainer = getEntityOrThrow(trainerId);
        return TrainerResponse.from(trainer);
    }


    // ========================================================================
    // READ  ---  list
    // ========================================================================
    @Override
    @Transactional(readOnly = true)
    public Page<TrainerResponse> getAllTrainers(TrainerRole role, Pageable pageable) {

        Page<Trainer> page = (role == null)
                ? trainerRepository.findByActiveTrue(pageable)
                : trainerRepository.findByRoleAndActiveTrue(role, pageable);

        /*
         * Page.map()
         * -------------------------------------------------------------
         * Page<Trainer> ko Page<TrainerResponse> me badal deta hai.
         *
         * Page ki baaki information (total pages, total elements,
         * current page) sab waise ki waisi rehti hai.
         *
         * Manually list banate to ye sab kho jata.
         */
        return page.map(TrainerResponse::from);
    }


    // ========================================================================
    // SEARCH  ---  naam se
    // ========================================================================
    @Override
    @Transactional(readOnly = true)
    public Page<TrainerResponse> searchByName(String name, Pageable pageable) {

        // Khali search = sab dikha do
        if (name == null || name.isBlank()) {
            return getAllTrainers(null, pageable);
        }

        return trainerRepository
                .findByNameContainingIgnoreCaseAndActiveTrue(name.trim(), pageable)
                .map(TrainerResponse::from);
    }


    // ========================================================================
    // UPDATE STATUS  ---  active / inactive
    // ========================================================================
    @Override
    public TrainerResponse updateStatus(String trainerId, boolean active) {

        Trainer trainer = getEntityOrThrow(trainerId);

        trainer.setActive(active);

        /*
         * save() call kar rahe hain — lekin sach ye hai ki
         * ISKI ZARURAT NAHI THI.
         *
         * Kyunki entity "managed" state me hai (transaction ke
         * andar DB se aayi hai). Transaction khatam hote hi
         * Hibernate khud dekh leta hai ki kya badla aur
         * UPDATE query maar deta hai. Isko "dirty checking" kehte hain.
         *
         * Phir bhi save() likh rahe hain kyunki code padhne
         * wale ko saaf dikhta hai ki yahan DB update ho raha hai.
         * Nuksaan koi nahi.
         */
        Trainer saved = trainerRepository.save(trainer);

        log.info("Trainer status updated | trainerId={} | active={}", trainerId, active);

        return TrainerResponse.from(saved);
    }


    // ========================================================================
    // INTERNAL  ---  doosri services ke liye
    // ========================================================================
    @Override
    @Transactional(readOnly = true)
    public Trainer getEntityOrThrow(String trainerId) {

        return trainerRepository.findByTrainerId(trainerId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Trainer", "trainerId", trainerId));
    }


    // ========================================================================
    // PRIVATE HELPER
    // ========================================================================
    /**
     * Optional field ko trim karta hai, null-safe.
     *
     * designation aur phone optional hain — null aa sakte hain,
     * to .trim() seedha lagane se NullPointerException aati.
     */
    private String trim(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }
}