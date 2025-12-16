package com.example.dat.role.service;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.example.dat.exceptions.NotFoundException;
import com.example.dat.res.Response;
import com.example.dat.role.entity.Role;
import com.example.dat.role.repo.RoleRepo;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RoleServiceImpl implements RoleService {

    private final RoleRepo roleRepo;


    @Override
    public Response<Role> createRole(Role roleRequest) {

        Role savedRole = roleRepo.save(roleRequest);

        return Response.<Role>builder()
                .statusCode(HttpStatus.OK.value())
                .message("Rol guardado correctamente")
                .data(savedRole)
                .build();
    }

    @Override
    public Response<Role> updateRole(Role roleRequest) {

        Role role = roleRepo.findById(roleRequest.getId())
            .orElseThrow(() -> new NotFoundException("Rol no encontrado"));

        role.setName(roleRequest.getName());

        Role updatedRole = roleRepo.save(role);
        return Response.<Role>builder()
                .statusCode(HttpStatus.OK.value())
                .message("Rol actualizado correctamente")
                .data(updatedRole)
                .build();
    }

    @Override
    public Response<List<Role>> getAllRoles() {

        List<Role> roles = roleRepo.findAll();
        return Response.<List<Role>>builder()
                .statusCode(HttpStatus.OK.value())
                .message("Roles obtenidos correctamente")
                .data(roles)
                .build();

    }

    @Override
    public Response<?> deleteRole(Long id) {
        if (!roleRepo.existsById(id)) {
            throw new NotFoundException("Rol no encontrado");
        }

        roleRepo.deleteById(id);

        return Response.builder()
                .statusCode(HttpStatus.OK.value())
                .message("Rol eliminado correctamente")
                .build();
    }
}
