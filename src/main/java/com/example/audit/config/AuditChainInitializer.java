package com.example.audit.config;

        import com.example.audit.entity.AuditChainState;
        import com.example.audit.repository.AuditChainStateRepository;
        import com.example.audit.service.HashChainService;
        import org.springframework.boot.CommandLineRunner;
        import org.springframework.context.annotation.Bean;
        import org.springframework.context.annotation.Configuration;

@Configuration
public class AuditChainInitializer {

    @Bean
    CommandLineRunner initializeAuditChain(
            AuditChainStateRepository repository,
            HashChainService hashChainService) {

        return args -> {
            if (!repository.existsById(1L)) {
                repository.saveAndFlush(
                        new AuditChainState(
                                1L,
                                0L,
                                hashChainService.getGenesisHash()
                        )
                );
            }
        };
    }
}

