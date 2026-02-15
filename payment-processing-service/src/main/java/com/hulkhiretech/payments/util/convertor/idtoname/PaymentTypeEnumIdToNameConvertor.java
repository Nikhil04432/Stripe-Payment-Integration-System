package com.hulkhiretech.payments.util.convertor.idtoname;

import com.hulkhiretech.payments.constant.PaymentMethodEnum;
import com.hulkhiretech.payments.constant.PaymentTypeEnum;
import org.modelmapper.AbstractConverter;

public class PaymentTypeEnumIdToNameConvertor extends AbstractConverter<Integer, String> {
    @Override
    protected String convert(Integer source) {
        return PaymentTypeEnum.fromId(source).getName();
    }
}