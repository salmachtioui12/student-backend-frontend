package com.example.authsec.token;

import com.example.authsec.user.User;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity

public class Token {

    @Id
    @GeneratedValue
    public Integer id;

    @Column(unique = true)
    public String token;

    //BEARER
    @Enumerated(EnumType.STRING)
    public TokenType tokenType = TokenType.BEARER;

    //token invalide, deconnecté ...
    public boolean revoked;

    public boolean expired;

    //pls tokens pour un user
    //LAZY => user n'est recuperer que si necessaire, on terme d'optimisation
    @ManyToOne(fetch = FetchType.LAZY)
    //clé etrangé dans table Token sera user_id
    @JoinColumn(name = "user_id")
    public User user;
}