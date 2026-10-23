package strategy;

import model.Loan;
import java.util.List;

public interface PayoffStrategy {
    /**
     * Sorts loans in the order in which prepayments should be allocated.
     *
     * @param loans the list of active loans to prioritize
     * @return a newly sorted list according to the strategy rules
     */
    List<Loan> sortLoans(List<Loan> loans);

    /**
     * Returns human-readable name of the strategy.
     */
    String getStrategyName();

    /**
     * Returns a concise description of the strategic priority rule.
     */
    String getDescription();
}
