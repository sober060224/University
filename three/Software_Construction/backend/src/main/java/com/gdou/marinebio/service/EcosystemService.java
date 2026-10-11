package com.gdou.marinebio.service;

import com.gdou.marinebio.common.BizException;
import com.gdou.marinebio.common.LoginUser;
import com.gdou.marinebio.dto.SpeciesForms;
import com.gdou.marinebio.entity.Ecosystem;
import com.gdou.marinebio.repository.EcosystemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 模块三：生态系统管理（珊瑚礁、红树林、海草床、潮间带岩礁、河口湿地、深海等）。
 */
@Service
@RequiredArgsConstructor
public class EcosystemService {

    private final EcosystemRepository ecosystemRepository;
    private final LogService logService;

    public List<Ecosystem> list() {
        return ecosystemRepository.findAll();
    }

    public Ecosystem get(Integer id) {
        return ecosystemRepository.findById(id)
                .orElseThrow(() -> new BizException("生态系统不存在"));
    }

    /** 智能问答用：问题里点名了哪个生态系统（「请总结红树林生态系统中……」） */
    public Ecosystem findMentionedIn(String question) {
        if (question == null || question.isBlank()) {
            return null;
        }
        // 取够候选之后在 Java 侧按名字长度倒序，让更具体的名字胜出
        // （「红树林」与「红树林湿地」同时命中时该用后者）。
        return ecosystemRepository.findMentionedIn(question.trim(),
                        org.springframework.data.domain.Pageable.ofSize(10)).stream()
                .max(java.util.Comparator.comparingInt(e -> e.getName() == null ? 0 : e.getName().length()))
                .orElse(null);
    }

    @Transactional
    public Ecosystem create(SpeciesForms.Ecosystem form, LoginUser operator) {
        if (ecosystemRepository.existsByName(form.name())) {
            throw new BizException("生态系统「" + form.name() + "」已存在");
        }
        Ecosystem ecosystem = new Ecosystem();
        apply(ecosystem, form, true);
        Ecosystem saved = ecosystemRepository.save(ecosystem);
        logService.record(operator, "模块三", "新增生态系统", "生态系统", saved.getId(), "新增生态系统「" + saved.getName() + "」");
        return saved;
    }

    @Transactional
    public Ecosystem update(Integer id, SpeciesForms.Ecosystem form, LoginUser operator) {
        Ecosystem ecosystem = get(id);
        if (form.name() != null && !form.name().isBlank()
                && !form.name().equals(ecosystem.getName())
                && ecosystemRepository.existsByName(form.name())) {
            throw new BizException("生态系统「" + form.name() + "」已存在");
        }
        apply(ecosystem, form, false);
        Ecosystem saved = ecosystemRepository.save(ecosystem);
        logService.record(operator, "模块三", "编辑生态系统", "生态系统", id, "编辑生态系统「" + saved.getName() + "」");
        return saved;
    }

    @Transactional
    public void delete(Integer id, LoginUser operator) {
        Ecosystem ecosystem = get(id);
        // observations.ecosystem_id 是 ON DELETE CASCADE，删除会连带清掉该生态系统下的观测记录
        ecosystemRepository.delete(ecosystem);
        logService.record(operator, "模块三", "删除生态系统", "生态系统", id,
                "删除生态系统「" + ecosystem.getName() + "」及其下属观测记录");
    }

    private void apply(Ecosystem target, SpeciesForms.Ecosystem form, boolean isCreate) {
        if (isCreate || notBlank(form.name())) target.setName(form.name());
        // 与 SpeciesService 保持同一套语义：传了才改，空串表示清空
        if (form.code() != null) target.setCode(form.code());
        if (form.description() != null) target.setDescription(form.description());
        if (form.imageUrl() != null) target.setImageUrl(form.imageUrl());
    }

    private static boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }
}
