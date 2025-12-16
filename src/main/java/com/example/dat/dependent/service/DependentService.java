package com.example.dat.dependent.service;

import com.example.dat.dependent.dto.DependentDTO;
import com.example.dat.res.Response;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface DependentService {

    Response<DependentDTO> registerDependent(Long patientId, DependentDTO dependentDTO);

    Response<List<DependentDTO>> getDependentsByPatient(Long patientId);

    Response<DependentDTO> getDependentById(Long dependentId);

    Response<DependentDTO> uploadProfilePhoto(Long dependentId, MultipartFile photo);

    Response<List<DependentDTO>> getMyDependents();
}
