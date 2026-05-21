package com.retrobolsa.api.validation;

import com.retrobolsa.api.user.dto.RegisterRequest;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class SenhasIguaisValidator implements ConstraintValidator<SenhasIguais, RegisterRequest> {

    @Override
    public boolean isValid(RegisterRequest dto, ConstraintValidatorContext context){
        return dto.getSenha() != null &&
                dto.getSenha().equals(dto.getConfirmarSenha());
    }


}
