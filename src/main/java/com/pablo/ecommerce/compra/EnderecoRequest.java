package com.pablo.ecommerce.compra;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record EnderecoRequest(
        @NotBlank @Pattern(regexp = "\\d{8}") String cep,
        @NotBlank String logradouro,
        @NotBlank String numero,
        String complemento,
        @NotBlank String bairro,
        @NotBlank String cidade,
        @NotBlank @Pattern(regexp = "[A-Z]{2}") String uf) {
    public EnderecoEntrega toEntity() {
        EnderecoEntrega endereco = new EnderecoEntrega();
        endereco.setCep(cep);
        endereco.setLogradouro(logradouro);
        endereco.setNumero(numero);
        endereco.setComplemento(complemento);
        endereco.setBairro(bairro);
        endereco.setCidade(cidade);
        endereco.setUf(uf);
        return endereco;
    }
}
