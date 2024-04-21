package com.zhu.project.manager;
import com.zhu.project.common.ErrorCode;
import com.zhu.project.exception.BusinessException;
import com.yupi.yucongming.dev.client.YuCongMingClient;
import com.yupi.yucongming.dev.common.BaseResponse;
import com.yupi.yucongming.dev.model.DevChatRequest;
import com.yupi.yucongming.dev.model.DevChatResponse;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
public class AiManager {
    @Resource
    private YuCongMingClient yuCongMingClient;

    /**
     * ai对话
     */
    public String doChat(Long modelId,String meg) {
        DevChatRequest devChatRequest = new DevChatRequest();
        devChatRequest.setModelId(modelId);
        devChatRequest.setMessage(meg);

        BaseResponse<DevChatResponse> response = yuCongMingClient.doChat(devChatRequest);
        if (response == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "AI响应错误");
        }
        return response.getData().getContent();
    }
}
