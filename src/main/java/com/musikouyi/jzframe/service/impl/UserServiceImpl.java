package com.musikouyi.jzframe.service.impl;

import com.musikouyi.jzframe.common.constant.Global;
import com.musikouyi.jzframe.common.exception.GlobalException;
import com.musikouyi.jzframe.dao.repository.DeptRepository;
import com.musikouyi.jzframe.dao.repository.RoleRepository;
import com.musikouyi.jzframe.dao.repository.UserRepository;
import com.musikouyi.jzframe.domain.entity.Result;
import com.musikouyi.jzframe.domain.entity.User;
import com.musikouyi.jzframe.domain.enums.ResultEnum;
import com.musikouyi.jzframe.domain.enums.SexEnum;
import com.musikouyi.jzframe.domain.enums.UserStatusEnum;
import com.musikouyi.jzframe.dto.ListReqDto;
import com.musikouyi.jzframe.dto.ListRespDto;
import com.musikouyi.jzframe.dto.UserReqDto;
import com.musikouyi.jzframe.dto.UserRespDto;
import com.musikouyi.jzframe.service.IUserService;
import com.musikouyi.jzframe.utils.ResultUtil;
import com.musikouyi.jzframe.utils.ToolsUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
public class UserServiceImpl implements IUserService {

    private final UserRepository userRepository;

    private final RoleRepository roleRepository;

    private final DeptRepository deptRepository;

    @Autowired
    public UserServiceImpl(UserRepository userRepository, RoleRepository roleRepository, DeptRepository deptRepository) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.deptRepository = deptRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public User getByAccount(String account) {
        return userRepository.findUserByAccount(account);
    }


    @Override
    @Transactional(readOnly = true)
    public Result findAll(ListReqDto listReqDto) {
        Page<User> userPage = userRepository.findByStatusIsNotAndAccountLike(UserStatusEnum.DELETED.getCode(), "%" + listReqDto.getQueryContent() + "%", PageRequest.of(listReqDto.getPage() - 1, listReqDto.getLimit()));
        List<User> userList = userPage.getContent();
        ListRespDto<UserRespDto> listRespDto = new ListRespDto<>();
        List<UserRespDto> userRespDtoList = new ArrayList<>();
        for (User user : userList) {
            UserRespDto userRespDto = new UserRespDto(
                    user.getId(),
                    user.getAccount(),
                    user.getName(),
                    SexEnum.fromCode(user.getSex()),
                    roleRepository.findById(user.getRoleId()).orElseThrow(() -> new GlobalException(ResultEnum.DATABASE_QUERRY_ERROR)).getName(),
                    deptRepository.findById(user.getDeptId()).orElseThrow(() -> new GlobalException(ResultEnum.DATABASE_QUERRY_ERROR)).getFullName(),
                    user.getEmail(),
                    user.getPhone(),
                    user.getCreateTime(),
                    UserStatusEnum.fromCode(user.getStatus())
            );
            userRespDtoList.add(userRespDto);
        }
        listRespDto.setItems(userRespDtoList);
        listRespDto.setTotal(userPage.getTotalElements());
        return ResultUtil.success(listRespDto);
    }

    @Override
    @Transactional
    public Result delete(Integer id) {
        if (Global.SUPER_USER_ID == id) {
            return ResultUtil.error(ResultEnum.FORBIDDEN);
        }
        User user = userRepository.findById(id).orElseThrow(() -> new GlobalException(ResultEnum.DATABASE_QUERRY_ERROR));
        user.setStatus(UserStatusEnum.DELETED.getCode());   //软删除，避免查找不到记录
        userRepository.saveAndFlush(user);
        return ResultUtil.success();
    }

    @Override
    @Transactional
    public Result create(UserReqDto userReqDto) {
        String salt = ToolsUtil.getRandomString(Global.SALT_LENGTH);

        User user = new User();
        user.setAccount(userReqDto.getAccount());
        user.setPhone(userReqDto.getPhone());
        user.setEmail(userReqDto.getEmail());
        user.setPassword(ToolsUtil.encrypt(userReqDto.getPassword(), salt));
        user.setName(userReqDto.getName());
        user.setStatus(UserStatusEnum.toCode(userReqDto.getStatus()));
        user.setSex(SexEnum.toCode(userReqDto.getSex()));
        user.setCreateTime(new Date());
        user.setSalt(salt);
        user.setRoleId(roleRepository.findIdByName(userReqDto.getRole()));
        user.setDeptId(deptRepository.findIdByName(userReqDto.getDept()));
        userRepository.saveAndFlush(user);
        return ResultUtil.success();
    }

    @Override
    @Transactional
    public Result modify(UserReqDto userReqDto) {
        User user = userRepository.findById(userReqDto.getId()).orElseThrow(() -> new GlobalException(ResultEnum.DATABASE_QUERRY_ERROR));
        user.setPhone(userReqDto.getPhone());
        user.setEmail(userReqDto.getEmail());
        user.setName(userReqDto.getName());
        user.setStatus(UserStatusEnum.toCode(userReqDto.getStatus()));
        user.setSex(SexEnum.toCode(userReqDto.getSex()));
        user.setRoleId(roleRepository.findIdByName(userReqDto.getRole()));
        user.setDeptId(deptRepository.findIdByName(userReqDto.getDept()));
        userRepository.saveAndFlush(user);
        return ResultUtil.success();
    }

    @Override
    @Transactional
    public Result changePwd(Integer id, String newPassword) {
        User user = userRepository.findById(id).orElseThrow(() -> new GlobalException(ResultEnum.DATABASE_QUERRY_ERROR));
        user.setPassword(ToolsUtil.encrypt(newPassword, user.getSalt()));
        userRepository.saveAndFlush(user);
        return ResultUtil.success();
    }
}
