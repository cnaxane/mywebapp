package strategy;

import model.Loan;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class AvalancheStrategy implements PayoffStrategy {

    @Override
    public List<Loan> sortLoans(List<Loan> loans) {
        if (loans == null) {
            return new ArrayList<>();
        }
        List<Loan> sorted = new ArrayList<>(loans);
        sorted.sort(Comparator
                .comparingDouble(Loan::getAnnualInterestRate).reversed()
                .thenComparing(Comparator.comparingDouble(Loan::getOutstandingBalance).reversed())
        );
        return sorted;
    }

    @Override
    public String getStrategyName() {
        return "Debt Avalanche";
    }

    @Override
    public String getDescription() {
        return "Prioritizes loans with the highest interest rate first to minimize lifetime interest charges.";
    }
}
