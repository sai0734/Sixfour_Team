package com.wedding.budget.service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import com.wedding.budget.domain.Budget;
import com.wedding.budget.dto.BudgetDTO;
import com.wedding.budget.repository.BudgetRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Service
@RequiredArgsConstructor
@Log4j2
public class BudgetServiceImpl implements BudgetService {

    private final BudgetRepository budgetRepository;

    private final ModelMapper modelMapper;

    @Override
    public Long register(BudgetDTO budgetDTO) {

        log.info("budget register.........");

        Budget budget = modelMapper.map(budgetDTO, Budget.class);

        Budget savedBudget = budgetRepository.save(budget);

        return savedBudget.getBudgetId();
    }

    @Override
    public void modify(BudgetDTO budgetDTO, String requesterEmail) {

        Optional<Budget> result = budgetRepository.findById(budgetDTO.getBudgetId());

        Budget budget = result.orElseThrow();

        if (!budget.getMemberEmail().equals(requesterEmail)) {
            throw new IllegalStateException("본인의 예산 항목만 수정할 수 있습니다.");
        }

        budget.changeCategory(budgetDTO.getCategory());
        budget.changeBudgetAmount(budgetDTO.getBudgetAmount());
        budget.changeActualAmount(budgetDTO.getActualAmount());
        budget.changeMemo(budgetDTO.getMemo());
        budget.changeSortOrder(budgetDTO.getSortOrder());

        budgetRepository.save(budget);
    }

    @Override
    public void remove(Long budgetId, String requesterEmail) {

        Optional<Budget> result = budgetRepository.findById(budgetId);

        Budget budget = result.orElseThrow();

        if (!budget.getMemberEmail().equals(requesterEmail)) {
            throw new IllegalStateException("본인의 예산 항목만 삭제할 수 있습니다.");
        }

        budgetRepository.deleteById(budgetId);
    }

    @Override
    public List<BudgetDTO> listByMember(String memberEmail) {

        List<Budget> result = budgetRepository.findByMemberEmailOrderBySortOrderAsc(memberEmail);

        return result.stream()
                .map(budget -> modelMapper.map(budget, BudgetDTO.class))
                .collect(Collectors.toList());
    }

}
