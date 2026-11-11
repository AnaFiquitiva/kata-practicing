package com.bdb.katapracticing.compartido;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ETagsTest {

    @Test
    void laVersionSePublicaComoEtagFuerte() {
        assertThat(ETags.de(3)).isEqualTo("\"3\"");
    }

    @ParameterizedTest
    @ValueSource(strings = {"\"3\"", "W/\"3\"", " 3 "})
    void leeLaVersionDeIfMatch(String ifMatch) {
        assertThat(ETags.versionDe(ifMatch)).isEqualTo(3L);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"*", "  "})
    void sinIfMatchOComodin_noExigeVersion(String ifMatch) {
        assertThat(ETags.versionDe(ifMatch)).isNull();
    }

    @Test
    void etagQueNoEsUnaVersion_esUnaPrecondicionFallida() {
        assertThatThrownBy(() -> ETags.versionDe("\"abc\"")).isInstanceOf(PrecondicionFallidaException.class);
    }
}
