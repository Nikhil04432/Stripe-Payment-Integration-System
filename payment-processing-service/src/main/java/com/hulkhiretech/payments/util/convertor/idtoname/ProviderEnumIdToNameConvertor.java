package com.hulkhiretech.payments.util.convertor.idtoname;

import com.hulkhiretech.payments.constant.ProviderEnum;
import org.modelmapper.AbstractConverter;

public class ProviderEnumIdToNameConvertor extends AbstractConverter<Integer, String> {
@Override
protected String convert(Integer source) {
        return ProviderEnum.fromId(source).getName();
        }
}