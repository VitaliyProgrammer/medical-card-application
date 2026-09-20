package com.vitaliy.medcard.configuration;

import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import java.time.format.DateTimeFormatter;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JacksonConfiguration {

    private static final DateTimeFormatter DATE_TIME_FORMAT =
            DateTimeFormatter.ofPattern("dd-MM-yyyy, HH:mm");
    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd-MM-yyyy");

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer dateTimeFormatCustomizer() {
        return builder -> builder
                .serializers(
                        new LocalDateTimeSerializer(DATE_TIME_FORMAT),
                        new LocalDateSerializer(DATE_FORMAT))
                .deserializers(
                        new LocalDateTimeDeserializer(DATE_TIME_FORMAT),
                        new LocalDateDeserializer(DATE_FORMAT));
    }
}
