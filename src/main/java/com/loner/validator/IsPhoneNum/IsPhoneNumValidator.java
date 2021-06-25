package com.loner.validator.IsPhoneNum;

import com.loner.utils.ValidatorUtil;
import com.loner.validator.IsPhoneNum.IsPhoneNum;
import org.thymeleaf.util.StringUtils;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public class IsPhoneNumValidator implements ConstraintValidator<IsPhoneNum, String> {
    private boolean required=true;
    @Override
    public void initialize(IsPhoneNum constraintAnnotation) {
        required=constraintAnnotation.required();

    }

    @Override
    public boolean isValid(String s, ConstraintValidatorContext constraintValidatorContext) {
        if (required){
            return true;
        }else {
            if (StringUtils.isEmpty(s)){
                return true;
            }else {
                return ValidatorUtil.isPhoneNum(s);
            }

        }
    }
}
