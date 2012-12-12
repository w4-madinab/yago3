package fromThemes;

import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;

import javatools.datatypes.FinalSet;
import basics.Fact;
import basics.Theme;

/**
 * YAGO2s - SPOTLXDeduplicator
 * 
 * Generates the final results of SPOTLX deduction,
 * extracting only occurs[In, Since, Until] relations
 * 
 * @author Joanna Biega
 */
public class SPOTLXDeduplicator extends SimpleDeduplicator {
  @Override
  public Set<Theme> input() {
    return new FinalSet<>(SPOTLXDeductiveExtractor.RULERESULTS);
  }
  
  public static final Theme SPOTLXFACTS = new Theme("spotlxFacts", "SPOTLX deduced facts");
  
  public static final List<String> SPOTLX_FINAL_RELATIONS = new ArrayList<String> (Arrays.asList(
        "<occursIn>",
        "<occursSince>",
        "<occursUntil>"
      ));
  
  @Override
  public Theme myOutput() {
    return SPOTLXFACTS;
  }

  @Override
  public boolean isMyRelation(Fact fact) {
    return SPOTLX_FINAL_RELATIONS.contains(fact.getRelation());
  }
}