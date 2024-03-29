package api.hackathon.iaiq.domain.board.service;


import api.hackathon.iaiq.domain.Member.domain.Member;
import api.hackathon.iaiq.domain.Member.service.MemberQueryService;
import api.hackathon.iaiq.domain.board.converter.BoardConverter;
import api.hackathon.iaiq.domain.board.domain.Board;
import api.hackathon.iaiq.domain.board.dto.BoardRequest;
import api.hackathon.iaiq.domain.board.dto.BoardResponse;
import api.hackathon.iaiq.domain.board.repository.BoardRepository;
import api.hackathon.iaiq.domain.boardCategory.domain.BoardCategory;
import api.hackathon.iaiq.domain.boardCategory.repository.BoardCategoryRepository;
import api.hackathon.iaiq.global.exception.ApiException;
import api.hackathon.iaiq.global.exception.ErrorType;
import api.hackathon.iaiq.global.utils.SecurityUtil;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static api.hackathon.iaiq.global.utils.SecurityUtil.getCurrentMember;

@Service
@RequiredArgsConstructor
@Transactional
public class BoardCommandService {

    private final EntityManager em;

    private final BoardRepository boardRepository;
    private final BoardCategoryRepository boardCategoryRepository;

    // 게시글 작성 로직
    public BoardResponse.BoardResultDTO writeBoard(BoardRequest.WriteDTO request) {
        BoardCategory boardCategory = boardCategoryRepository.findByTopic(request.getTopic()).orElseThrow(()-> new ApiException(ErrorType._BOARD_CATEGORY_NOT_FOUND));

        // 작성자 회원 조회
        Member loginMember = getCurrentMember();

        Board newBoard = BoardConverter.toBoard(request, boardCategory, loginMember);

        boardRepository.save(newBoard);
        return BoardConverter.toBoardResultDTO(newBoard);
    }

    //게시글 수정
    public BoardResponse.BoardEditResultDTO editBoard(Long boardId,BoardRequest.BoardEditDTO boardEditDTO) {
        Board board = boardRepository.findById(boardId).orElseThrow(()-> new ApiException(ErrorType._BOARD_NOT_FOUND));
        BoardCategory boardCategory = boardCategoryRepository.findByTopic(boardEditDTO.getTopic()).orElseThrow(()-> new ApiException(ErrorType._BOARD_CATEGORY_NOT_FOUND));


        //작성자인지 확인
        Member currentMember = SecurityUtil.getCurrentMember();

        if(!currentMember.getId().equals(board.getMember().getId()))
            throw new ApiException(ErrorType._NOT_BOARD_AUTHOR);

        //게시글 업데이트
        board.update(boardEditDTO.getTitle(),boardEditDTO.getContent(), boardCategory);

        em.flush();
        em.clear();

        Board updateBoard = boardRepository.findById(boardId).orElse(null);

        return BoardConverter.toBoardEditResultDTO(updateBoard);
    }

    // 게시글 삭제
    public BoardResponse.BoardResultDTO deleteBoard(Long boardId) {
        Board board = boardRepository.findById(boardId).orElseThrow(()-> new ApiException(ErrorType._BOARD_NOT_FOUND));

        //작성자인지 확인
        Member currentMember = SecurityUtil.getCurrentMember();
        if(!currentMember.getId().equals(board.getMember().getId()))
            throw new ApiException(ErrorType._NOT_BOARD_AUTHOR);
        
        //게시글 삭제
        boardRepository.deleteById(boardId);
        return BoardConverter.toBoardResultDTO(boardId);
    }
}
