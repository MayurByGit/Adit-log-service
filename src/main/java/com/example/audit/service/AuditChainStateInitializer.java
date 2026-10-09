
package com.example.audit.service;

import com.example.audit.entity.AuditChainState;
import com.example.audit.repository.AuditChainStateRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class AuditChainStateInitializer implements ApplicationRunner {

    private static final Long CHAIN_STATE_ID = 1L;

    private final AuditChainStateRepository stateRepository;
    private final HashChainService hashChainService;

    public AuditChainStateInitializer(
            AuditChainStateRepository stateRepository,
            HashChainService hashChainService) {
        this.stateRepository = stateRepository;
        this.hashChainService = hashChainService;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (stateRepository.findById(CHAIN_STATE_ID).isEmpty()) {
            AuditChainState initialState = new AuditChainState(
                    CHAIN_STATE_ID,
                    0L,
                    hashChainService.getGenesisHash());

            stateRepository.saveAndFlush(initialState);
        }
    }
}
