package com.hulkhiretech.payments.service.recon;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PaymentReconScheduler {

    private final ReconService reconService;

    @Scheduled(fixedDelay = 15 * 60 * 1000) // 15 minutes
    public void runRecon() {
        reconService.reconcilePendingTransactions();
    }
}
