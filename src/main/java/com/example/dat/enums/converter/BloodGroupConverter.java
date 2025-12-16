package com.example.dat.enums.converter;

import com.example.dat.enums.BloodGroup;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = false)
public class BloodGroupConverter implements AttributeConverter<BloodGroup, String> {

    @Override
    public String convertToDatabaseColumn(BloodGroup attribute) {
        if (attribute == null) return null;
        // Persist using the enum name (Spanish)
        return attribute.name();
    }

    @Override
    public BloodGroup convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) return null;
        String v = dbData.trim().toUpperCase();
        // Accept both legacy English and new Spanish values for backward compatibility
        switch (v) {
            case "A_POSITIVE":
            case "A_POSITIVO":
                return BloodGroup.A_POSITIVO;
            case "A_NEGATIVE":
            case "A_NEGATIVO":
                return BloodGroup.A_NEGATIVO;
            case "B_POSITIVE":
            case "B_POSITIVO":
                return BloodGroup.B_POSITIVO;
            case "B_NEGATIVE":
            case "B_NEGATIVO":
                return BloodGroup.B_NEGATIVO;
            case "AB_POSITIVE":
            case "AB_POSITIVO":
                return BloodGroup.AB_POSITIVO;
            case "AB_NEGATIVE":
            case "AB_NEGATIVO":
                return BloodGroup.AB_NEGATIVO;
            case "O_POSITIVE":
            case "O_POSITIVO":
                return BloodGroup.O_POSITIVO;
            case "O_NEGATIVE":
            case "O_NEGATIVO":
                return BloodGroup.O_NEGATIVO;
            default:
                // Unknown value; return null to avoid hard failure, or throw if preferred
                return null;
        }
    }
}
