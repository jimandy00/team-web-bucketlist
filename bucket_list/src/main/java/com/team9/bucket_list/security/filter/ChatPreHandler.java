package com.team9.bucket_list.security.filter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.team9.bucket_list.domain.enumerate.MemberRole;
import com.team9.bucket_list.execption.ApplicationException;
import com.team9.bucket_list.execption.ErrorCode;
import com.team9.bucket_list.security.utils.JwtUtil;
import io.jsonwebtoken.MalformedJwtException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;

import java.util.List;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class ChatPreHandler implements ChannelInterceptor {

    private final JwtUtil jwtUtil;
    Long memberId = 0L;
    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        try {
            log.info(message.toString());
            StompHeaderAccessor headerAccessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

            String authorizationHeader = String.valueOf(headerAccessor.getNativeHeader("Authorization"));

            StompCommand command = headerAccessor.getCommand();

            log.info("stomp command : "+ command);

            if(command.equals(StompCommand.UNSUBSCRIBE) || command.equals(StompCommand.SEND) || command.equals(StompCommand.MESSAGE)) return message;
            else if (command.equals(StompCommand.ERROR)) {
                throw new MessageDeliveryException("error");
            }


            log.info("jwt : " + authorizationHeader);

            if (authorizationHeader == null) {
                log.info("chat header??? ?????? ???????????????.");
                throw new MalformedJwtException("jwt");
            }

            //token ??????
            String token = "";
            String authorizationHeaderStr = authorizationHeader.replace("[","").replace("]","");
            if (authorizationHeaderStr.startsWith("Bearer ")) {
                token = authorizationHeaderStr.replace("Bearer ", "");
            } else {
                log.error("Authorization ?????? ????????? ????????????. : {}", authorizationHeader);
                throw new MalformedJwtException("jwt");
            }

            try {
                memberId = JwtUtil.getMemberId(token);
            } catch (JsonProcessingException e) {
                throw new MalformedJwtException("jwt");
            }

            boolean isTokenValid = jwtUtil.validateToken(token);

            if (isTokenValid) {
                this.setAuthentication(message, headerAccessor);
            }
        } catch (ApplicationException e) {
            log.error("JWT??????");
            throw new MalformedJwtException("jwt");
        } catch (MessageDeliveryException e) {
            log.error("????????? ??????");
            throw new MessageDeliveryException("error");
        }
        return message;
    }

    private void setAuthentication(Message<?> message, StompHeaderAccessor headerAccessor) {
        //??? ????????????, Role ?????????
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(memberId, null, List.of(new SimpleGrantedAuthority(MemberRole.USER.name())));
        SecurityContextHolder.getContext().setAuthentication(authentication);
        headerAccessor.setUser(authentication);
        log.info("?????? ?????? id???????????? : "+authentication.getName());
    }
}
