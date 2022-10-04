import java.io.BufferedReader;
import java.io.FileReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MovieAnalyzer {
    List<String[]> dataset = new ArrayList<>();

    public MovieAnalyzer(String dataset_path) {
        try (BufferedReader csvReader = new BufferedReader(new FileReader(dataset_path, StandardCharsets.UTF_8))) {
            String data;
            while ((data = csvReader.readLine()) != null) {
                String[] line = data.split(",(?=([^\"]*\"[^\"]*\")*[^\"]*$)", -1);
                String[] filteredLine = Stream.of(line).map(s -> s.startsWith("\"") ? s.substring(1, s.length() - 1) : s).toArray(String[]::new);
                dataset.add(filteredLine);
            }
            dataset.remove(0);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Map<Integer, Integer> getMovieCountByYear() {
        Map<Integer, Integer> map = dataset.stream()
                .filter(s -> s[2].length() != 0)
                .collect(Collectors.groupingBy(s -> Integer.parseInt(s[2]), Collectors.collectingAndThen(Collectors.counting(), Long::intValue)));

        Map<Integer, Integer> sortedMap = map.entrySet().stream()
                .sorted((e1, e2) -> e2.getKey().compareTo(e1.getKey()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
        return sortedMap;
    }

    public Map<String, Integer> getMovieCountByGenre() {
        Map<String, Integer> map = Arrays.stream(dataset.stream()
                .filter(s -> s[5].length() != 0)
                .map(s -> s[5].replace(" ", ""))
                .collect(Collectors.joining(","))
                .split(","))
                .collect(Collectors.groupingBy(Function.identity(), Collectors.collectingAndThen(Collectors.counting(), Long::intValue)));

        Map<String, Integer> sortedMap = map.entrySet().stream()
                .sorted((e1, e2) -> {
                    int c1 = e2.getValue().compareTo(e1.getValue());
                    return c1 != 0 ? c1 : e1.getKey().compareTo(e2.getKey());
                })
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));

        return sortedMap;
    }

    public Map<List<String>, Integer> getCoStarCount() {
        return dataset.stream().flatMap(s -> Stream.of(sortedCoStar(s[10], s[11]), sortedCoStar(s[10], s[12])
                , sortedCoStar(s[10], s[13]), sortedCoStar(s[11], s[12]), sortedCoStar(s[11], s[13]), sortedCoStar(s[12], s[13])))
                .collect(Collectors.groupingBy(Function.identity()
                        , Collectors.collectingAndThen(Collectors.counting(), Long::intValue)));

    }

    private List<String> sortedCoStar(String star1, String star2) {
        return Stream.of(star1, star2).sorted().collect(Collectors.toList());
    }

    public List<String> getTopMovies(int top_k, String by) {
        if ("runtime".equals(by)) {
            return dataset.stream().filter(s -> s[4].length() != 0).sorted((s1, s2) -> {
                int c1 = Integer.parseInt(s2[4].substring(0, s2[4].length() - 3).trim()) - Integer.parseInt(s1[4].substring(0, s1[4].length() - 3).trim());
                return c1 != 0 ? c1 : s1[1].compareTo(s2[1]);
            }).limit(top_k).map(s -> s[1]).collect(Collectors.toList());

        } else if ("overview".equals(by)) {
            return dataset.stream().filter(s -> s[7].length() != 0).sorted((s1, s2) -> {
                int c1 = s2[7].length() - s1[7].length();
                return c1 != 0 ? c1 : s1[1].compareTo(s2[1]);
            }).limit(top_k).map(s -> s[1]).collect(Collectors.toList());
        }

        return null;
    }

    public List<String> getTopStars(int top_k, String by) {
        if ("rating".equals(by)) {
            return dataset.stream().filter(s -> s[6].length() != 0)
                    .flatMap(s -> Stream.of(new String[]{s[6], s[10]}, new String[]{s[6], s[11]}
                            , new String[]{s[6], s[12]}, new String[]{s[6], s[13]}))
                    .collect(Collectors.groupingBy(s -> s[1], Collectors.averagingDouble(s -> Float.parseFloat(s[0]))))
                    .entrySet().stream()
                    .sorted((e1, e2) -> {
                        int c1 = Double.compare(e2.getValue(), e1.getValue());
                        return c1 != 0 ? c1 : e1.getKey().compareTo(e2.getKey());
                    }).limit(top_k).map(Map.Entry::getKey).collect(Collectors.toList());

        } else if ("gross".equals(by)) {
            return dataset.stream().filter(s -> s[15].length() != 0)
                    .flatMap(s -> Stream.of(new String[]{s[15], s[10]}, new String[]{s[15], s[11]}
                            , new String[]{s[15], s[12]}, new String[]{s[15], s[13]}))
                    .collect(Collectors.groupingBy(s -> s[1], Collectors.averagingLong(s -> Long.parseLong(s[0].replace(",", "")))))
                    .entrySet().stream()
                    .sorted((e1, e2) -> {
                        int c1 = e2.getValue().compareTo(e1.getValue());
                        return c1 != 0 ? c1 : e1.getKey().compareTo(e2.getKey());
                    }).limit(top_k).map(Map.Entry::getKey).collect(Collectors.toList());
        }

        return null;
    }

    public List<String> searchMovies(String genre, float min_rating, int max_runtime) {
        return dataset.stream().filter(s -> s[6].length() != 0 && Float.parseFloat(s[6]) >= min_rating)
                .filter(s -> s[4].length() != 0 && Integer.parseInt(s[4].substring(0, s[4].length() - 3).trim()) <= max_runtime)
                .filter(s -> s[5].length() != 0 && s[5].contains(genre))
                .map(s -> s[1])
                .sorted()
                .collect(Collectors.toList());

    }

}