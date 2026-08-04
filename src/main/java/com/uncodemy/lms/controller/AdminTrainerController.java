package com.uncodemy.lms.controller;

import com.uncodemy.lms.dto.ApiResponse;
import com.uncodemy.lms.dto.request.TrainerCreateRequest;
import com.uncodemy.lms.dto.response.TrainerCreateResponse;
import com.uncodemy.lms.dto.response.TrainerResponse;
import com.uncodemy.lms.model.enums.TrainerRole;
import com.uncodemy.lms.service.rule.TrainerService;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * ============================================================================
 * AdminTrainerController
 * ============================================================================
 *
 * Admin ke liye Trainer management ke endpoints.
 *
 * BASE URL : /api/admin/trainers
 *
 * ENDPOINTS
 * ---------------------------------------------------------------------------
 * POST   /api/admin/trainers                     -> naya trainer + mail
 * GET    /api/admin/trainers                     -> list (paginated)
 * GET    /api/admin/trainers/search              -> naam se dhundo
 * GET    /api/admin/trainers/check-username      -> username free hai?
 * GET    /api/admin/trainers/{trainerId}         -> ek trainer
 * PATCH  /api/admin/trainers/{trainerId}/status  -> chalu / band
 *
 * CONTROLLER KA KAAM SIRF ITNA HAI
 * ---------------------------------------------------------------------------
 *   1. Request lena
 *   2. Service ko dena
 *   3. Response wapas karna
 *
 * BUSINESS LOGIC YAHAN BILKUL NAHI.
 * Koi "if" nahi, koi calculation nahi, koi repository call nahi.
 *
 * TRY-CATCH KYUN NAHI HAI?
 * ---------------------------------------------------------------------------
 * Kyunki GlobalExceptionHandler sab sambhal leta hai.
 *
 * Yahan try-catch lagaya to exception wahin pakdi jayegi
 * aur handler tak pahunchegi hi nahi — poora Phase 1 ka
 * kaam bekaar ho jayega.
 *
 * "/api/admin/..." ka matlab abhi sirf naming hai.
 * Security abhi nahi hai, to ye endpoints sabke liye khule hain.
 * Security phase me @PreAuthorize("hasRole('ADMIN')") lagayenge.
 * ============================================================================
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/trainers")
@RequiredArgsConstructor
public class AdminTrainerController {

    private final TrainerService trainerService;


    // ========================================================================
    // 1. CREATE TRAINER   (API 1 + 2)
    // ========================================================================
    /**
     * POST /api/admin/trainers
     *
     * REQUEST BODY:
     * {
     *   "name"        : "Rahul Sharma",
     *   "username"    : "rahul.sharma",
     *   "email"       : "rahul@gmail.com",
     *   "designation" : "Senior Java Trainer",
     *   "phone"       : "9876543210",
     *   "role"        : "TRAINER"
     * }
     *
     * RESPONSE : 201 CREATED
     * {
     *   "success" : true,
     *   "message" : "Trainer created successfully",
     *   "data"    : {
     *       "trainer"           : { "trainerId": "TR101", ... },
     *       "temporaryPassword" : "Kf7@mQx2",
     *       "mailSentTo"        : "rahul@gmail.com"
     *   }
     * }
     *
     * @Valid  <-- YE ZAROORI HAI
     * -----------------------------------------------------------------------
     * Isko hatane se DTO ki saari validation
     * (@NotBlank, @Email, @Pattern) chup-chaap
     * ignore ho jayegi. Bahut common galti hai.
     *
     * 201 kyun, 200 kyun nahi?
     * -----------------------------------------------------------------------
     * 200 = "kaam ho gaya"
     * 201 = "nayi cheez ban gayi"
     *
     * Create ke liye 201 hi sahi HTTP status hai.
     */
    @PostMapping
    public ResponseEntity<ApiResponse<TrainerCreateResponse>> createTrainer(
            @Valid @RequestBody TrainerCreateRequest request) {

        log.info("POST /api/admin/trainers | username={}", request.getUsername());

        TrainerCreateResponse response = trainerService.createTrainer(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Trainer created successfully", response));
    }


    // ========================================================================
    // 2. USERNAME AVAILABLE HAI?
    // ========================================================================
    /**
     * GET /api/admin/trainers/check-username?username=rahul.sharma
     *
     * RESPONSE:
     * {
     *   "success" : true,
     *   "message" : "Username available hai",
     *   "data"    : true
     * }
     *
     * Frontend isse form me live check karega —
     * admin type karte-karte hi pata chal jayega.
     *
     * Ye hamesha 200 deta hai (404 ya 409 nahi),
     * kyunki "username exist karta hai" yahan ERROR nahi hai —
     * bas ek jawab hai.
     */
    @GetMapping("/check-username")
    public ResponseEntity<ApiResponse<Boolean>> checkUsername(
            @RequestParam String username) {

        boolean available = trainerService.isUsernameAvailable(username);

        String message = available
                ? "Username available hai"
                : "Ye username pehle se le liya gaya hai";

        return ResponseEntity.ok(ApiResponse.success(message, available));
    }


    // ========================================================================
    // 3. SAARE TRAINERS  (paginated)
    // ========================================================================
    /**
     * GET /api/admin/trainers
     * GET /api/admin/trainers?role=TRAINER
     * GET /api/admin/trainers?page=0&size=20&sort=name,asc
     *
     * @PageableDefault
     * -----------------------------------------------------------------------
     * Client ne page/size na bheja to ye default lagenge.
     *
     * Bina iske default size 20 hoti hai lekin sort nahi hota —
     * to har baar order badal sakta hai.
     *
     * Yahan "id DESC" rakha hai — sabse naya trainer upar.
     *
     * "required = false" on role
     * -----------------------------------------------------------------------
     * role optional hai. Na bheja to saare trainers aayenge.
     *
     * RESPONSE me Page ka pura structure aata hai:
     * {
     *   "content"       : [ ...trainers... ],
     *   "totalElements" : 47,
     *   "totalPages"    : 3,
     *   "number"        : 0,
     *   "size"          : 20
     * }
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Page<TrainerResponse>>> getAllTrainers(
            @RequestParam(required = false) TrainerRole role,
            @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.DESC)
            Pageable pageable) {

        Page<TrainerResponse> trainers = trainerService.getAllTrainers(role, pageable);

        return ResponseEntity.ok(
                ApiResponse.success("Trainers fetched successfully", trainers));
    }


    // ========================================================================
    // 4. NAAM SE SEARCH
    // ========================================================================
    /**
     * GET /api/admin/trainers/search?name=rah
     *
     * "rah" likho to "Rahul Sharma" aur "Prahlad" dono aayenge
     * (kahin bhi match ho jaye, chhota-bada letter matter nahi karta).
     *
     * NOTE: Ye API 10 ka SIMPLE version hai — sirf trainer
     * ke naam se. Phase 8 me pura search banega jisme
     * current topic se bhi dhoondh sakenge.
     */
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<Page<TrainerResponse>>> searchTrainers(
            @RequestParam(required = false) String name,
            @PageableDefault(size = 20, sort = "name", direction = Sort.Direction.ASC)
            Pageable pageable) {

        Page<TrainerResponse> trainers = trainerService.searchByName(name, pageable);

        return ResponseEntity.ok(
                ApiResponse.success("Search complete", trainers));
    }


    // ========================================================================
    // 5. EK TRAINER
    // ========================================================================
    /**
     * GET /api/admin/trainers/TR101
     *
     * Trainer na mile to:
     *   404 + "Trainer not found with trainerId : 'TR999'"
     *
     * Ye ResourceNotFoundException se apne aap ho jata hai —
     * yahan koi check likhne ki zarurat nahi.
     *
     * @PathVariable
     * -----------------------------------------------------------------------
     * URL ke {trainerId} wale hisse ko variable me daal deta hai.
     */
    @GetMapping("/{trainerId}")
    public ResponseEntity<ApiResponse<TrainerResponse>> getTrainer(
            @PathVariable String trainerId) {

        TrainerResponse trainer = trainerService.getByTrainerId(trainerId);

        return ResponseEntity.ok(
                ApiResponse.success("Trainer fetched successfully", trainer));
    }


    // ========================================================================
    // 6. STATUS BADLO  (chalu / band)
    // ========================================================================
    /**
     * PATCH /api/admin/trainers/TR101/status?active=false
     *
     * Trainer ko band karta hai (soft delete).
     * Uska data, batches, contents sab safe rehte hain.
     *
     * PUT nahi, PATCH kyun?
     * -----------------------------------------------------------------------
     * PUT   = poori cheez replace karo
     * PATCH = sirf ek hissa badlo   <-- yahi ho raha hai
     *
     * DELETE endpoint jaan-boojh kar nahi banaya —
     * trainer delete karne se uske batches, announcements
     * aur contents orphan ho jate ya foreign key error aata.
     */
    @PatchMapping("/{trainerId}/status")
    public ResponseEntity<ApiResponse<TrainerResponse>> updateStatus(
            @PathVariable String trainerId,
            @RequestParam boolean active) {

        log.info("PATCH status | trainerId={} | active={}", trainerId, active);

        TrainerResponse trainer = trainerService.updateStatus(trainerId, active);

        String message = active
                ? "Trainer activate kar diya gaya"
                : "Trainer deactivate kar diya gaya";

        return ResponseEntity.ok(ApiResponse.success(message, trainer));
    }
}