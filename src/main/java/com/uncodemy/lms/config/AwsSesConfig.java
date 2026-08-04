package com.uncodemy.lms.config;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sesv2.SesV2Client;

/**
 * ============================================================================
 * AwsSesConfig
 * ============================================================================
 *
 * Amazon SES (Simple Email Service) ka client bean banata hai.
 *
 * SES KYUN, Gmail SMTP KYUN NAHI?
 * ---------------------------------------------------------------------------
 * Gmail SMTP:
 *   ✘ Din me sirf ~500 mail
 *   ✘ Bulk bhejne pe account block ho jata hai
 *   ✘ Announcement 200 students ko gaya = spam folder
 *
 * Amazon SES:
 *   ✔ Lakhon mail, bahut sasta (~$0.10 per 1000)
 *   ✔ Domain verified hai to deliverability achhi
 *   ✔ Bounce / complaint ka track milta hai
 *
 * Humare project me API 8 (announcement) aur API 9 (content)
 * me ek saath poore batch ko mail jayegi — isliye SES hi sahi hai.
 *
 * CLIENT SINGLETON KYUN?
 * ---------------------------------------------------------------------------
 * SesV2Client ke andar HTTP connection pool hota hai.
 * Har mail pe naya client banate to har baar naya connection
 * banta — bahut slow aur memory leak ka risk.
 *
 * Isliye ek hi bean, poore app me reuse.
 * Ye thread-safe hai, to parallel mails me bhi problem nahi.
 * ============================================================================
 */
@Slf4j
@Configuration
public class AwsSesConfig {

    /**
     * AWS ka region.
     *
     * India ke liye ap-south-1 (Mumbai).
     *
     * IMPORTANT: Region wahi dena jahan tumne domain VERIFY kiya hai.
     * SES ka verification region-specific hota hai —
     * Mumbai me verify kiya aur code me Virginia diya
     * to "Email address not verified" error aayegi.
     */
    @Value("${aws.region}")
    private String region;

    @Value("${aws.access-key-id:}")
    private String accessKeyId;

    @Value("${aws.secret-access-key:}")
    private String secretAccessKey;


    /**
     * SES Client bean.
     */
    @Bean
    public SesV2Client sesV2Client() {

        log.info("AWS SES client bana raha hoon | region = {}", region);

        return SesV2Client.builder()
                .region(Region.of(region))
                .credentialsProvider(resolveCredentials())
                .build();
    }


    /**
     * Credentials kahan se lein — ye decide karta hai.
     *
     * DO TAREEKE
     * -----------------------------------------------------------------------
     *
     * 1. STATIC (local development)
     *    properties me access key + secret diya hai to wahi use hoga.
     *
     * 2. DEFAULT CHAIN (production — YEHI SAHI TAREEKA HAI)
     *    properties khali chhodo, to AWS SDK khud
     *    ye jagah dhundhta hai, isi order me:
     *
     *      a) Environment variables
     *      b) ~/.aws/credentials file
     *      c) EC2 / ECS ka IAM Role   <-- production me yahi
     *
     *    IAM Role sabse safe hai kyunki server pe koi
     *    key file rakhni hi nahi padti. AWS khud
     *    temporary credentials deta hai aur rotate bhi karta hai.
     */
    private AwsCredentialsProvider resolveCredentials() {

        boolean hasStaticKeys =
                accessKeyId != null && !accessKeyId.isBlank()
                        && secretAccessKey != null && !secretAccessKey.isBlank();

        if (hasStaticKeys) {
            log.warn("SES : static credentials use ho rahe hain. "
                    + "Production me IAM Role use karna.");

            return StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(accessKeyId, secretAccessKey));
        }

        log.info("SES : DefaultCredentialsProvider use ho raha hai "
                + "(env vars / ~/.aws/credentials / IAM Role)");

        return DefaultCredentialsProvider.create();
    }
}