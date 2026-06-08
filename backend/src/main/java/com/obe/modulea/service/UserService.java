package com.obe.modulea.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.obe.common.BizException;
import com.obe.common.PageResult;
import com.obe.modulea.dto.UserCreateRequest;
import com.obe.modulea.dto.UserUpdateRequest;
import com.obe.modulea.entity.SysRole;
import com.obe.modulea.entity.SysUser;
import com.obe.modulea.entity.TeachingClass;
import com.obe.modulea.mapper.SysRoleMapper;
import com.obe.modulea.mapper.SysUserMapper;
import com.obe.modulea.mapper.TeachingClassMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;
    private final PasswordEncoder passwordEncoder;
    private final TeachingClassMapper teachingClassMapper;

    public PageResult<SysUser> listUsers(long page, long size, String keyword, Long roleId, Integer status) {
        var wrapper = new LambdaQueryWrapper<SysUser>();
        if (keyword != null && !keyword.isBlank()) {
            wrapper.and(w -> w.like(SysUser::getUsername, keyword)
                    .or().like(SysUser::getRealName, keyword));
        }
        if (roleId != null) {
            wrapper.eq(SysUser::getRoleId, roleId);
        }
        if (status != null) {
            wrapper.eq(SysUser::getStatus, status);
        }
        wrapper.orderByDesc(SysUser::getCreatedAt);

        Page<SysUser> result = userMapper.selectPage(new Page<>(page, size), wrapper);

        // Load role codes and names
        List<Long> roleIds = result.getRecords().stream().map(SysUser::getRoleId).distinct().toList();
        Map<Long, SysRole> roleMap = roleIds.isEmpty() ? Map.of()
                : roleMapper.selectBatchIds(roleIds).stream()
                        .collect(Collectors.toMap(SysRole::getId, r -> r));

        result.getRecords().forEach(u -> {
            u.setPasswordHash(null);
            SysRole role = roleMap.get(u.getRoleId());
            if (role != null) {
                u.setRoleCode(role.getRoleCode());
                u.setRoleName(role.getRoleName());
            }
        });

        return new PageResult<>(result.getRecords(), result.getTotal(), result.getCurrent(), result.getSize());
    }

    public SysUser getUserById(Long id) {
        SysUser user = userMapper.selectById(id);
        if (user == null) {
            throw new BizException("用户不存在");
        }
        user.setPasswordHash(null);
        return user;
    }

    public void createUser(UserCreateRequest request) {
        Long count = userMapper.selectCount(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, request.getUsername()));
        if (count > 0) {
            throw new BizException("用户名已存在");
        }

        SysUser user = new SysUser();
        user.setUsername(request.getUsername());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setRealName(request.getRealName());
        user.setRoleId(request.getRoleId());
        user.setCollegeId(request.getCollegeId());
        user.setStatus(1);
        userMapper.insert(user);
    }

    public void updateUser(Long id, UserUpdateRequest request) {
        SysUser user = userMapper.selectById(id);
        if (user == null) {
            throw new BizException("用户不存在");
        }
        user.setRealName(request.getRealName());
        user.setRoleId(request.getRoleId());
        user.setCollegeId(request.getCollegeId());
        if (request.getStatus() != null) {
            user.setStatus(request.getStatus());
        }
        userMapper.updateById(user);
    }

    public void resetPassword(Long id, String newPassword) {
        SysUser user = userMapper.selectById(id);
        if (user == null) {
            throw new BizException("用户不存在");
        }
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userMapper.updateById(user);
    }

    public void disableUser(Long id, Long currentUserId) {
        if (id != null && id.equals(currentUserId)) {
            throw new BizException("不能禁用当前登录用户");
        }
        SysUser user = userMapper.selectById(id);
        if (user == null) {
            throw new BizException("用户不存在");
        }
        user.setStatus(0);
        userMapper.updateById(user);
    }

    public void hardDeleteUser(Long id, Long currentUserId) {
        if (id != null && id.equals(currentUserId)) {
            throw new BizException("不能删除当前登录用户");
        }
        SysUser user = userMapper.selectById(id);
        if (user == null) {
            throw new BizException("用户不存在");
        }
        Long classCount = teachingClassMapper.selectCount(
                new LambdaQueryWrapper<TeachingClass>().eq(TeachingClass::getTeacherId, id));
        if (classCount > 0) throw new BizException("该用户是 " + classCount + " 个教学班级的主讲教师，无法删除");
        userMapper.deleteById(id);
    }

    public void toggleUserStatus(Long id) {
        SysUser user = userMapper.selectById(id);
        if (user == null) {
            throw new BizException("用户不存在");
        }
        user.setStatus(user.getStatus() == 1 ? 0 : 1);
        userMapper.updateById(user);
    }
}
