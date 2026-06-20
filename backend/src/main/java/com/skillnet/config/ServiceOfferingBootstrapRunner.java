package com.skillnet.config;



import com.fasterxml.jackson.databind.node.JsonNodeFactory;

import com.skillnet.persistence.entity.core.InfoproductorServiceOffering;

import com.skillnet.persistence.entity.core.User;

import com.skillnet.persistence.repository.InfoproductorServiceOfferingRepository;

import com.skillnet.persistence.repository.UserRepository;

import com.skillnet.service.entitlement.ServiceEntitlementService;

import java.math.BigDecimal;

import java.time.Instant;

import java.util.List;

import lombok.RequiredArgsConstructor;

import lombok.extern.slf4j.Slf4j;

import org.springframework.boot.CommandLineRunner;

import org.springframework.stereotype.Component;

import org.springframework.transaction.annotation.Transactional;



/** Planes IA por defecto (Gamma + ElevenLabs) y cuota inicial por infoproductor. */

@Slf4j

@Component

@RequiredArgsConstructor

public class ServiceOfferingBootstrapRunner implements CommandLineRunner {



    private final InfoproductorServiceOfferingRepository serviceOfferingRepository;

    private final UserRepository userRepository;

    private final ServiceEntitlementService entitlementService;



    @Override

    @Transactional

    public void run(String... args) {

        seedIfMissing(

                ServiceEntitlementService.CAPABILITY_GAMMA_EBOOK,

                "Generación de ebooks con IA (Gamma)",

                "Crea ebooks profesionales con IA. Incluye exportación PDF desde Gamma.",

                new BigDecimal("9.99"),

                "ri-book-open-line",

                ServiceEntitlementService.STARTER_GAMMA_EBOOK_USES);

        seedIfMissing(

                ServiceEntitlementService.CAPABILITY_PODCAST_AI,

                "Generación de podcasts con IA (ElevenLabs)",

                "Convierte tus ideas en episodios de podcast con voz natural usando ElevenLabs.",

                new BigDecimal("14.99"),

                "ri-mic-line",

                ServiceEntitlementService.STARTER_PODCAST_AI_USES);



        syncOfferingIncludedUses(

                ServiceEntitlementService.CAPABILITY_GAMMA_EBOOK,

                ServiceEntitlementService.STARTER_GAMMA_EBOOK_USES);

        syncOfferingIncludedUses(

                ServiceEntitlementService.CAPABILITY_PODCAST_AI,

                ServiceEntitlementService.STARTER_PODCAST_AI_USES);



        grantStarterEntitlementsToInfoproductors();

    }



    private void seedIfMissing(

            String capabilityKey,

            String title,

            String description,

            BigDecimal priceUsd,

            String iconClass,

            int includedUses) {

        if (serviceOfferingRepository.existsByCapabilityKey(capabilityKey)) {

            return;

        }

        Instant now = Instant.now();

        InfoproductorServiceOffering offering = new InfoproductorServiceOffering();

        offering.setSection("ia");

        offering.setTitle(title);

        offering.setDescription(description);

        offering.setPriceUsd(priceUsd);

        offering.setIconClass(iconClass);

        offering.setSortOrder(capabilityKey.equals(ServiceEntitlementService.CAPABILITY_GAMMA_EBOOK) ? 1 : 2);

        offering.setActive(true);

        offering.setFeatures(JsonNodeFactory.instance.arrayNode().add("Pago seguro con Stripe"));

        offering.setFeatured(true);

        offering.setCapabilityKey(capabilityKey);

        offering.setIncludedUses(includedUses);

        offering.setCreatedAt(now);

        offering.setUpdatedAt(now);

        serviceOfferingRepository.save(offering);

        log.info("Plan IA creado: {} ({}) — {} usos", title, capabilityKey, includedUses);

    }



    private void syncOfferingIncludedUses(String capabilityKey, int includedUses) {

        serviceOfferingRepository

                .findByCapabilityKey(capabilityKey)

                .ifPresent(offering -> {

                    if (offering.getIncludedUses() != includedUses) {

                        offering.setIncludedUses(includedUses);

                        offering.setUpdatedAt(Instant.now());

                        serviceOfferingRepository.save(offering);

                        log.info("Plan IA actualizado: {} → {} usos incluidos", capabilityKey, includedUses);

                    }

                });

    }



    private void grantStarterEntitlementsToInfoproductors() {

        InfoproductorServiceOffering gammaOffering = serviceOfferingRepository

                .findByCapabilityKey(ServiceEntitlementService.CAPABILITY_GAMMA_EBOOK)

                .orElse(null);

        InfoproductorServiceOffering podcastOffering = serviceOfferingRepository

                .findByCapabilityKey(ServiceEntitlementService.CAPABILITY_PODCAST_AI)

                .orElse(null);

        if (gammaOffering == null && podcastOffering == null) {

            return;

        }



        List<User> infoproductors = userRepository.findByInfoproductorTrue();

        int gammaGranted = 0;

        int podcastGranted = 0;

        for (User user : infoproductors) {

            if (gammaOffering != null) {

                int before = entitlementService.capabilitySummary(user.getId())

                        .get(ServiceEntitlementService.CAPABILITY_GAMMA_EBOOK)

                        .getUsesRemaining();

                entitlementService.grantStarterEntitlement(

                        user, gammaOffering, ServiceEntitlementService.STARTER_GAMMA_EBOOK_USES);

                entitlementService.bumpActiveQuotaMinimum(

                        user.getId(),

                        ServiceEntitlementService.CAPABILITY_GAMMA_EBOOK,

                        ServiceEntitlementService.STARTER_GAMMA_EBOOK_USES);

                int after = entitlementService.capabilitySummary(user.getId())

                        .get(ServiceEntitlementService.CAPABILITY_GAMMA_EBOOK)

                        .getUsesRemaining();

                if (after > before) {

                    gammaGranted++;

                }

            }

            if (podcastOffering != null) {

                int before = entitlementService.capabilitySummary(user.getId())

                        .get(ServiceEntitlementService.CAPABILITY_PODCAST_AI)

                        .getUsesRemaining();

                entitlementService.grantStarterEntitlement(

                        user, podcastOffering, ServiceEntitlementService.STARTER_PODCAST_AI_USES);

                entitlementService.bumpActiveQuotaMinimum(

                        user.getId(),

                        ServiceEntitlementService.CAPABILITY_PODCAST_AI,

                        ServiceEntitlementService.STARTER_PODCAST_AI_USES);

                int after = entitlementService.capabilitySummary(user.getId())

                        .get(ServiceEntitlementService.CAPABILITY_PODCAST_AI)

                        .getUsesRemaining();

                if (after > before) {

                    podcastGranted++;

                }

            }

        }

        if (gammaGranted > 0 || podcastGranted > 0) {

            log.info(

                    "Cuotas IA iniciales otorgadas — ebooks: {} usuarios, podcasts: {} usuarios",

                    gammaGranted,

                    podcastGranted);

        }

    }

}


