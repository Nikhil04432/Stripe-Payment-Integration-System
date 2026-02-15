package com.hulkhiretech.payments.util.convertor.nametoid;

import com.hulkhiretech.payments.constant.PaymentMethodEnum;
import org.modelmapper.AbstractConverter;

public class PaymentMethodEnumConvertor extends AbstractConverter<String, Integer> {
    @Override
    protected Integer convert(String source) {
        return PaymentMethodEnum.fromName(source).getId();
    }
}
