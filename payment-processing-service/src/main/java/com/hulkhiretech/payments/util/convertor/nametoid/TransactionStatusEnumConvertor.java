package com.hulkhiretech.payments.util.convertor.nametoid;

import com.hulkhiretech.payments.constant.PaymentMethodEnum;
import com.hulkhiretech.payments.constant.TransactionStatusEnum;
import org.modelmapper.AbstractConverter;

public class TransactionStatusEnumConvertor extends AbstractConverter<String, Integer> {
    @Override
    protected Integer convert(String source) {
        return TransactionStatusEnum.fromName(source).getId();
    }
}
