package com.github.dev001hajipro.twime;

import com.codepoetics.protonpack.StreamUtils;
import twitter4j.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Twimeメイン
 * Created by dev001 on 2017/02/28.
 */
public class App {
    // コンストラクター
    private App() {
    }

    public static void main(String[] args) {
        App app = new App();
        app.run();
    }

    private void run() {
        System.out.println("[twime] please input command or twitter @name.");
        System.out.println("  q is quit.");
        System.out.println("  a is show account");
        System.out.println("  @name is get media url and video url.");
        System.out.print("> ");

        // 無限の標準入力ストリーム
        try (Stream<String> sin = new BufferedReader(new InputStreamReader(System.in)).lines()) {
            sin.forEach(line -> {
                System.out.println(line);
                if ("q".equals(line)) {
                    return;
                }

                if ("a".equals(line)) {
                    String accountInfo = getAccount().map(u -> {
                        String buff = "";
                        buff += "screen name:" + u.getScreenName();
                        buff += "name       :" + u.getName();
                        buff += "Description:" + u.getDescription();
                        return buff;
                    }).orElse("account not found.");
                    System.out.println(accountInfo);
                }

                if (line.startsWith("@")) {
                    if (line.length() <= 2) {
                        System.out.println("unknown name. " + line);
                    }
                    // Twitterからデータ取得
                    getTwitterUser(line).ifPresent(user -> {
                        List<Status> statusList = getStatuses(user).collect(Collectors.toList());
                        // 画像メディア
                        List<String> oneImages = statusList.stream()
                                .filter(status -> status.getMediaEntities().length > 0)
                                .flatMap(status -> Arrays.stream(status.getMediaEntities()).map(MediaEntity::getMediaURL))
                                .collect(Collectors.toList());
                        oneImages.forEach(System.out::println);
                        // 動画メディア
                        List<String> videoUrls = statusList.stream()
                                .filter(status -> status.getMediaEntities().length > 0)
                                .flatMap(status -> Arrays.stream(status.getMediaEntities())
                                        .filter(e -> e.getVideoVariants().length > 0)
                                        .flatMap(v -> Arrays.stream(v.getVideoVariants()).map(MediaEntity.Variant::getUrl))
                                ).collect(Collectors.toList());
                        videoUrls.forEach(System.out::println);
                    });
                }
            });
        }
    }

    private Optional<User> getTwitterUser(String screenName) {
        Twitter twitter = TwitterFactory.getSingleton();
        try {
            return Optional.ofNullable(twitter.showUser(screenName));
        } catch (TwitterException e) {
            e.printStackTrace();
            return Optional.empty();
        }
    }

    private Optional<User> getAccount() {
        Twitter twitter = TwitterFactory.getSingleton();
        try {
            return Optional.ofNullable(twitter.verifyCredentials());
        } catch (TwitterException e) {
            e.printStackTrace();
            return Optional.empty();
        }
    }

    private Stream<Status> getStatuses(User user) {
        Twitter twitter = TwitterFactory.getSingleton();
        Stream<Paging> originStream = Stream.iterate(new Paging(1, 100), p -> {
            p.setPage(p.getPage() + 1);
            return p;
        });
        return StreamUtils.takeWhile(originStream, p -> {
            boolean hasPage;
            try {
                hasPage = !twitter.getUserTimeline(user.getId(), p).isEmpty();
            } catch (TwitterException e) {
                throw new UncheckedIOException(new IOException(e));
            }
            return hasPage;
        }).flatMap(p -> {
            List<Status> status;
            try {
                status = twitter.getUserTimeline(user.getId(), p);
            } catch (TwitterException e) {
                throw new UncheckedIOException(new IOException(e));
            }
            return status.stream();
        });
    }
}
