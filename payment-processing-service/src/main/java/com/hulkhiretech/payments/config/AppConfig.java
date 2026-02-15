package com.hulkhiretech.payments.config;

import com.hulkhiretech.payments.dto.TransactionDTO;
import com.hulkhiretech.payments.entity.TransactionEntity;
import com.hulkhiretech.payments.util.convertor.idtoname.PaymentMethodEnumIdToNameConvertor;
import com.hulkhiretech.payments.util.convertor.idtoname.PaymentTypeEnumIdToNameConvertor;
import com.hulkhiretech.payments.util.convertor.idtoname.ProviderEnumIdToNameConvertor;
import com.hulkhiretech.payments.util.convertor.idtoname.TransactionStatusEnumIdToNameConvertor;
import com.hulkhiretech.payments.util.convertor.nametoid.PaymentMethodEnumConvertor;
import com.hulkhiretech.payments.util.convertor.nametoid.PaymentTypeEnumConvertor;
import com.hulkhiretech.payments.util.convertor.nametoid.ProviderEnumConvertor;
import com.hulkhiretech.payments.util.convertor.nametoid.TransactionStatusEnumConvertor;
import org.modelmapper.Converter;
import org.modelmapper.ModelMapper;
import org.modelmapper.PropertyMap;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AppConfig {
    @Bean
    ModelMapper modelMapper() {
        ModelMapper modelMapper = new ModelMapper();
        modelMapper.getConfiguration()
                .setMatchingStrategy(MatchingStrategies.STRICT)
                .setSkipNullEnabled(true);

        // Only map when field names match exactly
        modelMapper.getConfiguration()
                .setMatchingStrategy(MatchingStrategies.STRICT)
                .setSkipNullEnabled(true); // Optional: ignore nulls

        Converter<String, Integer> paymentMethodEnumConverter = new PaymentMethodEnumConvertor();
        Converter<String, Integer> providerEnumConverter = new ProviderEnumConvertor();
        // Define converters for TxnStatusEnum and PaymentTypeEnum if needed
        Converter<String, Integer> paymentTypeEnumConverter = new PaymentTypeEnumConvertor();
        Converter<String, Integer> transactionStatusEnumConverter = new TransactionStatusEnumConvertor();


        modelMapper.addMappings(new PropertyMap<TransactionDTO, TransactionEntity>() {
            @Override
            protected void configure() {
                using(paymentMethodEnumConverter).map(source.getPaymentMethod(), destination.getPaymentMethodId());
                using(providerEnumConverter).map(source.getProvider(), destination.getProviderId());
                // Add mappings for txnStatusId and paymentTypeId with their respective converters
                using(paymentTypeEnumConverter).map(source.getPaymentType(), destination.getPaymentTypeId());
                using(transactionStatusEnumConverter).map(source.getTxnStatus(), destination.getTxnStatusId());
            }
        });

        Converter<Integer, String> paymentMethodEnumIdToNameConverter = new PaymentMethodEnumIdToNameConvertor();
        Converter<Integer, String> providerEnumIdToNameConvertor = new ProviderEnumIdToNameConvertor();
        // Define converters for TxnStatusEnum and PaymentTypeEnum if needed
        Converter<Integer, String> paymentTypeEnumIdToNameConverter = new PaymentTypeEnumIdToNameConvertor();
        Converter<Integer, String> transactionStatusEnumIdToNameConverter = new TransactionStatusEnumIdToNameConvertor();


        modelMapper.addMappings(new PropertyMap<TransactionEntity, TransactionDTO>() {
            @Override
            protected void configure() {
                using(paymentMethodEnumIdToNameConverter).map(source.getPaymentMethodId(), destination.getPaymentMethod());
                using(providerEnumIdToNameConvertor).map(source.getProviderId(), destination.getProvider());
                // Add mappings for txnStatusId and paymentTypeId with their respective converters
                using(paymentTypeEnumIdToNameConverter).map(source.getPaymentTypeId(), destination.getPaymentType());
                using(transactionStatusEnumIdToNameConverter).map(source.getTxnStatusId(), destination.getTxnStatus());
            }
        });

        return modelMapper;
    }
}

