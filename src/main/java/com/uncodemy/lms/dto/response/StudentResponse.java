package com.uncodemy.lms.dto.response;

import com.uncodemy.lms.model.Student;
import com.uncodemy.lms.model.StudentBatch;
import com.uncodemy.lms.model.enums.ApprovalStatus;
import com.uncodemy.lms.model.enums.StudentStatus;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * ============================================================================
 * StudentResponse   ---  Student ka OUTPUT
 * ============================================================================
 *
 * {
 *   "studentId"      : "STU101",
 *   "name"           : "Adarsh Jha",
 *   "email"          : "adarsh@gmail.com",
 *   "course"         : "Java Full Stack",
 *   "status"         : "ACTIVE",
 *   "approvalStatus" : "APPROVED",
 *   "addedByTrainer" : "TR101",
 *   "batches" : [
 *      { "batchId": "JAVA101", "batchName": "Java Full Stack",
 *        "trainerName": "Rahul Sharma", "joinedAt": "2026-07-31 10:00" }
 *   ]
 * }
 *
 * PASSWORD YAHAN NAHI HAI — na plain, na hash.
 * ============================================================================
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentResponse {

    private Long id;
    private String studentId;
    private String name;
    private String email;
    private String phone;
    private String course;

    /** ACTIVE / INACTIVE / BLOCKED */
    private StudentStatus status;

    /**
     * PENDING  -> trainer ne add kiya, admin approve karega
     * APPROVED -> chalu
     */
    private ApprovalStatus approvalStatus;

    /** Kis trainer ne add kiya (admin ne kiya to null) */
    private String addedByTrainer;

    /** Kitne batches me hai */
    private Integer totalBatches;

    /** Batch ki details (sirf detail API me bharti hai) */
    private List<EnrolledBatch> batches;

    private LocalDateTime createdAt;


    /**
     * Nested class — student ke ek batch ki info.
     *
     * Poora BatchResponse nahi bhej rahe kyunki usme
     * meetLink, certificateLink jaisa bahut kuch hai
     * jo yahan bekaar hai.
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class EnrolledBatch {
        private String batchId;
        private String batchName;
        private String timing;
        private String currentTopic;
        private String trainerName;
        private LocalDateTime joinedAt;
    }


    // ========================================================================
    // MAPPERS
    // ========================================================================

    /** Simple version — list ke liye (batch details nahi) */
    public static StudentResponse from(Student student) {
        return from(student, null, null);
    }

    /**
     * Full version.
     *
     * @param enrollments StudentBatch ki list (null ho sakti hai)
     */
    public static StudentResponse from(Student student,
                                       List<StudentBatch> enrollments,
                                       Integer totalBatches) {

        if (student == null) {
            return null;
        }

        String addedBy = (student.getAddedByTrainer() != null)
                ? student.getAddedByTrainer().getTrainerId() : null;

        List<EnrolledBatch> batchList = null;

        if (enrollments != null) {
            batchList = enrollments.stream()
                    .map(sb -> EnrolledBatch.builder()
                            .batchId(sb.getBatch().getBatchId())
                            .batchName(sb.getBatch().getBatchName())
                            .timing(sb.getBatch().getTiming())
                            .currentTopic(sb.getBatch().getCurrentTopic())
                            .trainerName(sb.getBatch().getTrainer() != null
                                    ? sb.getBatch().getTrainer().getName() : null)
                            .joinedAt(sb.getJoinedAt())
                            .build())
                    .toList();
        }

        return StudentResponse.builder()
                .id(student.getId())
                .studentId(student.getStudentId())
                .name(student.getName())
                .email(student.getEmail())
                .phone(student.getPhone())
                .course(student.getCourse())
                .status(student.getStatus())
                .approvalStatus(student.getApprovalStatus())
                .addedByTrainer(addedBy)
                .totalBatches(totalBatches != null ? totalBatches
                        : (batchList != null ? batchList.size() : null))
                .batches(batchList)
                .createdAt(student.getCreatedAt())
                .build();
    }
}