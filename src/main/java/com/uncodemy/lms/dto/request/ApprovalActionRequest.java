package com.uncodemy.lms.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import lombok.*;

/**
 * ============================================================================
 * ApprovalActionRequest   ---  Admin ke approve / reject ka INPUT
 * ============================================================================
 *
 * POST /api/admin/approvals/{id}/approve
 * POST /api/admin/approvals/{id}/reject
 *
 * Sample:
 * {
 *   "adminId" : "ADM101",
 *   "remark"  : "Is naam ka batch already chal raha hai"
 * }
 *
 * adminId YAHAN KYUN HAI?
 * ---------------------------------------------------------------------------
 * Kyunki abhi login/security nahi hai — server ko pata hi
 * nahi ki request kis admin ne bheji.
 *
 * Aur humein DB me save karna hai ki KISNE approve kiya
 * (reviewedByAdmin field).
 *
 * Security lagne ke baad ye field HAT JAYEGI — tab
 * JWT token se admin apne aap pata chal jayega.
 *
 * remark KAB ZAROORI HAI?
 * ---------------------------------------------------------------------------
 * APPROVE me : optional
 * REJECT  me : ZAROORI  <-- service check karegi
 *
 * Kyunki trainer ko pata chalna chahiye ki mana kyun kiya.
 * Bina reason ke reject karna trainer ke liye frustrating hai.
 *
 * Ye check @NotBlank se nahi ho sakta (kyunki approve me
 * optional hai), isliye service me manually hoga.
 * ============================================================================
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApprovalActionRequest {

    /**
     * Kaunsa admin ye action le raha hai.
     *
     * Security aane par ye field hat jayegi.
     */
    @NotBlank(message = "Admin ID zaroori hai")
    private String adminId;

    /**
     * Admin ka note.
     *
     * REJECT me zaroori, APPROVE me optional.
     */
    @Size(max = 500, message = "Remark 500 character se lamba nahi ho sakta")
    private String remark;
}