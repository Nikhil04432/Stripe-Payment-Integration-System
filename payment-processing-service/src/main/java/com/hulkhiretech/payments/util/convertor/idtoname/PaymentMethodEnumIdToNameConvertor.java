package com.hulkhiretech.payments.util.convertor.idtoname;

import com.hulkhiretech.payments.constant.PaymentMethodEnum;
import org.modelmapper.AbstractConverter;

public class PaymentMethodEnumIdToNameConvertor extends AbstractConverter<Integer, String> {
    @Override
    protected String convert(Integer source) {
        return PaymentMethodEnum.fromId(source).getName();
    }
}
