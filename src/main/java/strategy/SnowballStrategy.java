package strategy;

import model.Loan;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class SnowballStrategy implements PayoffStrategy {

    @Override
    public List<Loan> sortLoans(List<Loan> loans) {
        if (loans == null) {
            return new ArrayList<>();
        }
        List<Loan> sorted = new ArrayList<>(loans);
        sorted.sort(Comparator
                .comparingDouble(Loan::getOutstandingBalance)
                .thenComparing(Comparator.comparingDouble(Loan::getAnnualInterestRate).reversed())
        );
        return sorted;
    }

    @Override
    public String getStrategyName() {
        return "Debt Snowball";
    }

    @Override
    public String getDescription() {
        return "Prioritizes loans with the smallest outstanding balances first to build psychological momentum and eliminate monthly obligations.";
    }
}
