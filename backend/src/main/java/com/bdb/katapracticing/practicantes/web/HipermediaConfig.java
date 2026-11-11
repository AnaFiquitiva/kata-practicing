package com.bdb.katapracticing.practicantes.web;

import com.bdb.katapracticing.practicantes.dominio.EstadoPracticante;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.hateoas.config.EnableHypermediaSupport;
import org.springframework.hateoas.config.EnableHypermediaSupport.HypermediaType;
import org.springframework.hateoas.mediatype.hal.forms.HalFormsConfiguration;
import org.springframework.hateoas.mediatype.hal.forms.HalFormsOptions;

/**
 * REST nivel 3: además de enlaces HAL, la API publica plantillas HAL-FORMS ({@code _templates})
 * que describen cómo ejecutar cada acción (método, tipo de contenido, campos y valores posibles).
 * El cliente las obtiene con {@code Accept: application/prs.hal-forms+json}.
 */
@Configuration
@EnableHypermediaSupport(type = {HypermediaType.HAL, HypermediaType.HAL_FORMS})
class HipermediaConfig {

    @Bean
    HalFormsConfiguration halFormsConfiguration() {
        return new HalFormsConfiguration()
                .withOptions(ActualizarEstadoRequest.class, "estado",
                        metadata -> HalFormsOptions.inline((Object[]) EstadoPracticante.values()).withMinItems(1L).withMaxItems(1L));
    }
}
