package com.ai4.dict.service;

import com.ai4.common.exception.BizException;
import com.ai4.common.exception.ErrorCode;
import com.ai4.dict.dto.CreateDictTypeRequest;
import com.ai4.dict.dto.UpdateDictTypeRequest;
import com.ai4.dict.model.DictType;
import com.ai4.dict.repository.DictTypeRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class DictTypeService {

    private final DictTypeRepository dictTypeRepository;

    public DictTypeService(DictTypeRepository dictTypeRepository){
        this.dictTypeRepository = dictTypeRepository;
    }

    //UUID.randomUUID.toString()

    /**
     *根据id查询字典类型
     */
    public DictType get(String typeId){
        return dictTypeRepository.findById(typeId)
                .orElseThrow(() -> new BizException(ErrorCode.DICT_TYPE_NOT_FOUND));
    }

    /**
     * 根据可选条件查询字典类型
     */
    public List<DictType> getAll(String dictName, String dictCode, Integer status){
        return dictTypeRepository.findAll(dictName,dictCode,status);
    }

    /**
     * 创建字典类型
     */
    public DictType create(CreateDictTypeRequest request){

        if(dictTypeRepository.existByDictCode(request.dictCode())){
            throw new BizException(ErrorCode.DICT_CODE_ALREADY_EXISTS);
        }

        String typeId = UUID.randomUUID().toString();
        int status = request.status() == null ? 0 : request.status();
        DictType dictType = new DictType(
                typeId,
                request.dictName().trim(),
                request.dictCode().trim(),
                status,
                request.remark(),
                null,
                null
        );

        dictTypeRepository.save(dictType);
        return get(typeId);
    }

    /**
     * 修改字典类型
     */
    public DictType update(String typeId, UpdateDictTypeRequest request){
        DictType existing = get(typeId);

        DictType updated = new DictType(
                existing.id(),
                request.dictName(),
                existing.dictCode(),
                existing.status(),
                request.remark(),
                existing.createdAt(),
                existing.updatedAt()
        );
        int affectedRows = dictTypeRepository.update(updated);

        if(affectedRows == 0){
            throw new BizException(ErrorCode.DICT_TYPE_NOT_FOUND);
        }
        return get(typeId);
    }
}
