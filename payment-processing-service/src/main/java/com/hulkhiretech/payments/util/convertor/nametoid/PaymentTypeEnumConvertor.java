package com.hulkhiretech.payments.util.convertor.nametoid;

import com.hulkhiretech.payments.constant.PaymentMethodEnum;
import com.hulkhiretech.payments.constant.PaymentTypeEnum;
import org.modelmapper.AbstractConverter;

public class PaymentTypeEnumConvertor extends AbstractConverter<String, Integer> {
    @Override
    protected Integer convert(String source) {
        return PaymentTypeEnum.fromName(source).getId();
    }
}
