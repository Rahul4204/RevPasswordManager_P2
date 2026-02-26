package com.passwordmanager.app.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "pm_security_questions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SecurityQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sq_seq")
    @SequenceGenerator(name = "sq_seq", sequenceName = "pm_sq_seq", allocationSize = 1)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "question_text", nullable = false, length = 255)
    private String questionText;

    // BCrypt-hashed answer
    @Column(name = "answer_hash", nullable = false, length = 255)
    private String answerHash;
}
