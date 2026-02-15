package com.hulkhiretech.payments.util.convertor.idtoname;

import com.hulkhiretech.payments.constant.PaymentMethodEnum;
import com.hulkhiretech.payments.constant.TransactionStatusEnum;
import org.modelmapper.AbstractConverter;

public class TransactionStatusEnumIdToNameConvertor extends AbstractConverter<Integer, String> {
    @Override
    protected String convert(Integer source) {
        return TransactionStatusEnum.fromId(source).getName();
    }
}
