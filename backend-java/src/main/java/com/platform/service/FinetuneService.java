package com.platform.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.platform.entity.ai.FinetuneTask;
import com.platform.mapper.ai.FinetuneTaskMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class FinetuneService {

    private final FinetuneTaskMapper finetuneTaskMapper;

    public Page<FinetuneTask> pageQuery(int page, int pageSize, String status) {
        LambdaQueryWrapper<FinetuneTask> wrapper = new LambdaQueryWrapper<>();
        if (status != null && !status.isBlank()) {
            wrapper.eq(FinetuneTask::getStatus, status);
        }
        wrapper.orderByDesc(FinetuneTask::getCreatedAt);
        return finetuneTaskMapper.selectPage(new Page<>(page, pageSize), wrapper);
    }

    public FinetuneTask getById(Long id) {
        return finetuneTaskMapper.selectById(id);
    }

    public FinetuneTask createTask(Map<String, Object> body) {
        FinetuneTask t = new FinetuneTask();
        t.setTaskName((String) body.get("taskName"));
        t.setBaseModel((String) body.get("baseModel"));
        t.setDatasetName((String) body.get("datasetName"));
        t.setDatasetSize(body.get("datasetSize") != null
                ? ((Number) body.get("datasetSize")).intValue() : 0);
        t.setEpochs(body.get("epochs") != null
                ? ((Number) body.get("epochs")).intValue() : 3);
        t.setLearningRate(body.get("learningRate") != null
                ? ((Number) body.get("learningRate")).doubleValue() : 0.001);
        t.setStatus("pending");
        t.setProgress(0);
        t.setCurrentEpoch(0);
        finetuneTaskMapper.insert(t);
        return t;
    }

    public void cancelTask(Long id) {
        FinetuneTask t = finetuneTaskMapper.selectById(id);
        if (t != null) {
            t.setStatus("cancelled");
            finetuneTaskMapper.updateById(t);
        }
    }
}
