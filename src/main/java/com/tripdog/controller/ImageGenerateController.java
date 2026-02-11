package com.tripdog.controller;

import com.tripdog.common.Result;
import com.tripdog.model.dto.ImageGenerateReqDTO;
import com.tripdog.model.dto.ImageGenerateRespDTO;
import com.tripdog.service.ImageGenerateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@Tag(name = "生图")
@RestController
@RequestMapping("/image")
@RequiredArgsConstructor
public class ImageGenerateController {
	private final ImageGenerateService imageGenerateService;

	@Operation(summary = "ComfyUI 生图", description = "调用本地 ComfyUI workflow 生成图片")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "生成成功")
	})
	@PostMapping("/generate")
	public Result<ImageGenerateRespDTO> generate(@RequestBody ImageGenerateReqDTO request) {
		return Result.success(imageGenerateService.generate(request));
	}
}
