package radiolab;

import java.io.File;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class RadiolabScraper {
	
	public static void main(String[] args) {
		
		RadiolabScraper scaper = new RadiolabScraper();
		scaper.run();
		
	}
	
	public void run() {
		
		// seasons
		int[] getTheseSeason = {};
		for (int getThis : getTheseSeason) {
			this.getRadioLabSeason(getThis);	
		}
		
		// podcasts
		this.getRadioLabPodcasts();

		

		
	}
	
	protected void getRadioLabPodcasts() {
		
		try {
			Document doc = Jsoup.connect("http://www.radiolab.org/series/podcasts/").get();
			
			// find how many pages there are
			int podcastPages = Integer.parseInt(doc.select(".pagefooter-link").last().text());

			// scrape pages n to 2
			for(int n = podcastPages; n > 1; n--){
				Document page = Jsoup.connect("http://www.radiolab.org/series/podcasts/" + n).get();
				this.scrapePodcastListingPage(page);
			}
			// scrape page 1
			this.scrapePodcastListingPage(doc);

			System.out.println("Done with podcasts.");	
		
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * http://www.radiolab.org/series/podcasts/
	 * @param doc
	 */
	protected void scrapePodcastListingPage(Document doc) {
		
		try {

			Elements links = doc.select(".series-item h2.title a");
			
			for (Element l : links){
				String href = l.attr("href");
				Document detailsPage = Jsoup.connect(href).get();
				this.scrapeStory(detailsPage);
			}
		
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	protected void scrapeStory(Document detailsPage){

		try {
			// skip this is it's a season episode
			String se = detailsPage.select(".seanum-epnum").text();
			if (!se.equals("")){
				System.out.println("Skipping, this is an episode: " + detailsPage.baseUri());
				return;
			}
			
			String title = detailsPage.select(".article-full .title").first().text();
			String date = this.parseDate(detailsPage.select(".article-full .date").first().text());
			
			String filepath = "Downloaded";
			filepath += File.separator;
			filepath += "Podcasts";
			filepath += File.separator;			
			
			
			String filename = date;
			filename += " - ";
			filename += title;
			filename += ".mp3";
					
			Element player = detailsPage.select(".inline_audioplayer_wrapper").first();
			if (player == null){
				System.out.println("Skipping, no player on this page: " + title);
				return;
			}
			
			String xmlDocUrl = player.select(".player_element").first().attr("data-url");
			Document xmlDoc = Jsoup.connect(xmlDocUrl).get();
			String downloadLink = xmlDoc.select("location").text();
			
			URL downloadUrl = new URL(downloadLink);
			
			File saveFile = new File(filepath+filename);
			
			if (saveFile.exists()){
				System.out.println("Skipping, file already exists: " + filename);
				return;
			}
			System.out.println("Downloading " + filename + " ...");	
			org.apache.commons.io.FileUtils.copyURLToFile(downloadUrl, saveFile);
			
		
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}
	
	protected String parseDate(String dateString){
		
		try {
			SimpleDateFormat parseFormat = new SimpleDateFormat("EEEE, MMMM d, yyyy");
			Date date = parseFormat.parse(dateString);
			
			SimpleDateFormat toFormat = new SimpleDateFormat("yyyy-MM-dd");
			String formattedDate = toFormat.format(date);
			
			return formattedDate;
			
		} catch (Exception e) {
			
			e.printStackTrace();
			return "";
		}
		
	}
	

	
	protected void getRadioLabSeason(int season){
		
		try {
			
			int i = 0;
			Document doc = Jsoup.connect("http://www.radiolab.org/archive/").get();
			
			Elements seasons = doc.select(".interior-lead");
						
			for (Element s : seasons) {
				
				String seasonString = s.select("h1").first().text();
				int seasonNum = Integer.parseInt(seasonString.replaceAll("\\D", ""));
				
				if (seasonNum != season){
					continue;
				}
						
				Elements episodes = s.select("#radiolab-archive-list li");
				
				// System.out.println(season);
				
				for (Element e : episodes) {

					String href = e.select(".read-more").attr("href");
					String title= e.select("h4").first().text();
					
//					System.out.println(title);
//					System.out.println(href);
					
					this.getRadioLabEpisode(href);
					
					i++;
				}
				
			}
			System.out.println("Done with " + i + " episodes");
			
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	protected void getRadioLabEpisode(String href) {
		
		try {
			Document doc = Jsoup.connect(href).get();
			
			String se = doc.select(".seanum-epnum").text();
			String[] seArray = se.split("\\|");

			// build filename
			String season = seArray[0].replaceAll("[^0-9]", "");
			String episode = seArray[1].replaceAll("[^0-9]", "");
			String title = doc.select(".story-headergroup h2.title").text();
			
			String filepath = "Downloaded";
			filepath += File.separator;
			filepath += "Season " + season;
			filepath += File.separator;			
			
			
			String filename = season;
			filename += String.format("%02d", Integer.parseInt(episode));
			filename += " - ";
			filename += title;
			filename += ".mp3";
			
			// escape chars for windows filename:
			
			filename = this.getValidFileName(filename);
			
			// download file. find the mp3 link in this trickey xml file for the player
			String xmlDocUrl = doc.select(".inline_audioplayer_wrapper").first().select(".player_element").first().attr("data-url");
			Document xmlDoc = Jsoup.connect(xmlDocUrl).get();
			String downloadLink = xmlDoc.select("location").text();
			
			URL downloadUrl = new URL(downloadLink);
			File saveFile = new File(filepath+filename);
			
			System.out.println("Downloading " + filename + " ...");	
			org.apache.commons.io.FileUtils.copyURLToFile(downloadUrl, saveFile);
				
			
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}
	
	
	// not robust, but works for this.
	private String getValidFileName(String fileName) {
	    String newFileName = fileName.replaceAll("\\?|\\!", "");
	    if(newFileName.length()==0)
	        throw new IllegalStateException(
	                "File Name " + fileName + " results in a empty fileName!");
	    return newFileName;
	}

}
