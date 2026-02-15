package com.hulkhiretech.payments.util.convertor.nametoid;

import com.hulkhiretech.payments.constant.PaymentMethodEnum;
import com.hulkhiretech.payments.constant.ProviderEnum;
import org.modelmapper.AbstractConverter;

public class ProviderEnumConvertor extends AbstractConverter<String, Integer> {
    @Override
    protected Integer convert(String source) {
        return ProviderEnum.fromName(source).getId();
    }
}
