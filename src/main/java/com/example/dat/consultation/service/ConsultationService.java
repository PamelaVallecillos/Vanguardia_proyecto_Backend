package com.example.dat.consultation.service;

import com.example.dat.consultation.dto.ConsultationDTO;
import com.example.dat.res.Response;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ConsultationService {

    Response<ConsultationDTO> createConsultation(ConsultationDTO consultationDTO);

    Response<ConsultationDTO> getConsultationByAppointmentId(Long appointmentId);

    Response<List<ConsultationDTO>> getConsultationHistoryForPatient(Long patientId);

    Response<?> uploadConsultationDocuments(Long consultationId, List<MultipartFile> files);

    Response<List<ConsultationDTO>> getMyConsultations();

}
