package com.rick.scaffold.core.service.generic;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import com.rick.scaffold.core.dao.generic.BaseDao;
import com.rick.scaffold.core.entity.generic.BaseEntity;

/**
 * Service基类
 */
@Transactional(readOnly = true)
public abstract class BaseService<D extends BaseDao<T>, T extends BaseEntity<T>> {

	/**
	 * 持久层对象
	 */
	@Autowired
	protected D dao;
	
	/**
	 * 获取单条数据
	 * @param id
	 * @return
	 */
	public T findOne(Long id) {
		return dao.findOne(id);
	}
	
	/**
	 * 查询列表数据
	 * @param entity
	 * @return
	 */
	public List<T> findAll() {
		return dao.findAll();
	}

	/**
	 * 保存数据（插入或更新）
	 * @param entity
	 */
	@Transactional(readOnly = false)
	public int save(T entity) {
		return dao.insert(entity);
	}
	
	/**
	 * 更新数据（插入或更新）
	 * @param entity
	 */
	@Transactional(readOnly = false)
	public int update(T entity) {
		return dao.update(entity);
	}
	
	/**
	 * 删除数据
	 * @param entity
	 */
	@Transactional(readOnly = false)
	public int delete(Long id) {
		return dao.delete(id);
	}
}
