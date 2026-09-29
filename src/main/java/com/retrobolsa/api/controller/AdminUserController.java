package com.retrobolsa.api.controller;

import com.retrobolsa.api.game.dto.AdminUserDto;
import com.retrobolsa.api.service.AdminUserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/users")
public class AdminUserController {

    private final AdminUserService adminUserService;

    /** Busca por username ou e-mail; sem busca, lista os professores. */
    @GetMapping
    public ResponseEntity<List<AdminUserDto>> search(@RequestParam(required = false) String q) {
        return ResponseEntity.ok(adminUserService.search(q));
    }

    /** Promove a professor (TEACHER) ou volta a jogador (PLAYER). */
    @PostMapping("/{id}/role")
    public ResponseEntity<AdminUserDto> changeRole(@PathVariable UUID id, @Valid @RequestBody RoleRequest request) {
        return ResponseEntity.ok(adminUserService.changeRole(id, request.role()));
    }

    public record RoleRequest(@NotBlank(message = "Informe o papel: PLAYER ou TEACHER.") String role) {}
}
