package com.pablo.ecommerce.compra;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record EnderecoRequest(
        @NotBlank @Pattern(regexp = "\\d{8}") String cep,
        @NotBlank @Size(max = 120) String logradouro,
        @NotBlank @Size(max = 20) String numero,
        @Size(max = 120) String complemento,
        @NotBlank @Size(max = 80) String bairro,
        @NotBlank @Size(max = 80) String cidade,
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
