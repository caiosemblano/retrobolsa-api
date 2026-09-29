package com.retrobolsa.api.game.classroom;

import com.retrobolsa.api.exception.NotFoundException;
import com.retrobolsa.api.game.dto.ClassroomDto;
import com.retrobolsa.api.game.dto.StudentClassroomDto;
import com.retrobolsa.api.user.User;
import com.retrobolsa.api.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Turmas: o professor cria, arquiva e troca o código; o aluno entra com o código
 * e sai quando quiser. Um professor só enxerga as próprias turmas: a de outro
 * professor responde como se não existisse.
 */
@Service
@RequiredArgsConstructor
public class ClassroomService {

    private static final int MAX_CODE_ATTEMPTS = 20;

    private final ClassroomRepository classroomRepository;
    private final ClassroomMemberRepository memberRepository;
    private final UserRepository userRepository;
    private final Clock clock;

    // -------------------------------------------------------------------------
    // Professor
    // -------------------------------------------------------------------------

    @Transactional
    public ClassroomDto create(UUID teacherId, String name, String institution) {
        Classroom classroom = classroomRepository.save(Classroom.builder()
                .name(name.trim())
                .institution(institution == null || institution.isBlank() ? null : institution.trim())
                .teacherId(teacherId)
                .joinCode(newCode())
                .createdAt(LocalDateTime.now(clock))
                .build());
        return toTeacherDto(classroom, 0);
    }

    @Transactional(readOnly = true)
    public List<ClassroomDto> teacherClassrooms(UUID teacherId) {
        List<Classroom> classrooms = classroomRepository.findAllByTeacherIdOrderByArchivedAscCreatedAtDesc(teacherId);
        Map<UUID, Long> counts = memberCounts(classrooms.stream().map(Classroom::getId).toList());
        return classrooms.stream()
                .map(c -> toTeacherDto(c, counts.getOrDefault(c.getId(), 0L)))
                .toList();
    }

    @Transactional
    public ClassroomDto setArchived(UUID teacherId, UUID classroomId, boolean archived) {
        Classroom classroom = owned(teacherId, classroomId);
        classroom.setArchived(archived);
        return toTeacherDto(classroomRepository.save(classroom), memberCount(classroomId));
    }

    /** Código novo: o antigo deixa de valer (por exemplo, se vazou para fora da sala). */
    @Transactional
    public ClassroomDto regenerateCode(UUID teacherId, UUID classroomId) {
        Classroom classroom = owned(teacherId, classroomId);
        classroom.setJoinCode(newCode());
        return toTeacherDto(classroomRepository.save(classroom), memberCount(classroomId));
    }

    /** A turma, se for deste professor; a de outro professor é tratada como inexistente. */
    @Transactional(readOnly = true)
    public Classroom owned(UUID teacherId, UUID classroomId) {
        return classroomRepository.findById(classroomId)
                .filter(c -> c.getTeacherId().equals(teacherId))
                .orElseThrow(() -> new NotFoundException("Turma não encontrada"));
    }

    // -------------------------------------------------------------------------
    // Aluno
    // -------------------------------------------------------------------------

    @Transactional
    public StudentClassroomDto join(UUID userId, String typedCode) {
        String code = JoinCodes.normalize(typedCode);
        Classroom classroom = classroomRepository.findByJoinCode(code)
                .orElseThrow(() -> new IllegalArgumentException("Código de turma inválido. Confira com o professor."));
        if (classroom.isArchived()) {
            throw new IllegalArgumentException("Esta turma foi arquivada pelo professor.");
        }
        if (classroom.getTeacherId().equals(userId)) {
            throw new IllegalArgumentException("Você é o professor desta turma.");
        }
        memberRepository.insertIfAbsent(classroom.getId(), userId, LocalDateTime.now(clock));
        return mine(userId).stream()
                .filter(c -> c.getId().equals(classroom.getId().toString()))
                .findFirst()
                .orElseThrow();
    }

    @Transactional
    public void leave(UUID userId, UUID classroomId) {
        ClassroomMember.Id id = new ClassroomMember.Id(classroomId, userId);
        if (!memberRepository.existsById(id)) {
            throw new IllegalArgumentException("Você não está nesta turma.");
        }
        memberRepository.deleteById(id);
    }

    /** As turmas ativas do aluno. */
    @Transactional(readOnly = true)
    public List<StudentClassroomDto> mine(UUID userId) {
        Map<UUID, LocalDateTime> joinedAt = memberRepository.findAllByIdUserId(userId).stream()
                .collect(Collectors.toMap(m -> m.getId().getClassroomId(), ClassroomMember::getJoinedAt));
        if (joinedAt.isEmpty()) return List.of();

        List<Classroom> classrooms = classroomRepository.findAllByIdInAndArchivedFalseOrderByName(joinedAt.keySet());
        Map<UUID, Long> counts = memberCounts(joinedAt.keySet());
        Map<UUID, String> teachers = userRepository.findAllById(
                        classrooms.stream().map(Classroom::getTeacherId).collect(Collectors.toSet()))
                .stream().collect(Collectors.toMap(User::getId, User::getUsername));

        return classrooms.stream()
                .map(c -> StudentClassroomDto.builder()
                        .id(c.getId().toString())
                        .name(c.getName())
                        .institution(c.getInstitution())
                        .teacherUsername(teachers.get(c.getTeacherId()))
                        .memberCount(counts.getOrDefault(c.getId(), 0L))
                        .joinedAt(joinedAt.get(c.getId()))
                        .build())
                .toList();
    }

    // -------------------------------------------------------------------------
    // Ranking da turma
    // -------------------------------------------------------------------------

    /** Só o professor da turma e os alunos dela podem ver o ranking filtrado por ela. */
    @Transactional(readOnly = true)
    public boolean canSeeRanking(UUID userId, UUID classroomId) {
        return classroomRepository.findById(classroomId)
                .map(c -> c.getTeacherId().equals(userId)
                        || memberRepository.existsById(new ClassroomMember.Id(classroomId, userId)))
                .orElse(false);
    }

    @Transactional(readOnly = true)
    public Set<UUID> memberIds(UUID classroomId) {
        return memberRepository.memberIds(classroomId);
    }

    // -------------------------------------------------------------------------
    // Auxiliares
    // -------------------------------------------------------------------------

    private String newCode() {
        for (int i = 0; i < MAX_CODE_ATTEMPTS; i++) {
            String code = JoinCodes.random();
            if (!classroomRepository.existsByJoinCode(code)) return code;
        }
        throw new IllegalStateException("Não foi possível gerar um código de turma livre");
    }

    private long memberCount(UUID classroomId) {
        return memberCounts(List.of(classroomId)).getOrDefault(classroomId, 0L);
    }

    private Map<UUID, Long> memberCounts(Collection<UUID> ids) {
        if (ids.isEmpty()) return Map.of();
        return memberRepository.countByClassroomIds(ids).stream()
                .collect(Collectors.toMap(ClassroomMemberRepository.MemberCount::getClassroomId,
                        ClassroomMemberRepository.MemberCount::getTotal));
    }

    private ClassroomDto toTeacherDto(Classroom c, long members) {
        return ClassroomDto.builder()
                .id(c.getId().toString())
                .name(c.getName())
                .institution(c.getInstitution())
                .joinCode(c.getJoinCode())
                .archived(c.isArchived())
                .memberCount(members)
                .createdAt(c.getCreatedAt())
                .build();
    }
}
