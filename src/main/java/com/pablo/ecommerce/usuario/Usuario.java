package com.pablo.ecommerce.usuario;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter 
public class Usuario {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Setter 
    @NotBlank
    String nome;

    @Setter 
    @NotBlank
    @Email
    String email;

    @JsonIgnore
    @Setter 
    @NotBlank
    String senha;

    @Enumerated(EnumType.STRING)
    @Setter
    Papel papel = Papel.CLIENTE;

    public Papel getPapel() {
        // Accounts created before roles existed are regular customers.
        return papel == null ? Papel.CLIENTE : papel;
    }

}
