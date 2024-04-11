import pq;

class Dijkstra {
    int[] distances;
    int[] visited;
    int[] previous;

    int dijkstra(int u) {
        // incorrect dijkstra but good semantic testing
        int v;
        int i;
        int to;
        int weight;
        int[] adj;
        int[] lenghts;
        int[] weights;
        distances[u] = 0;
        pq = new pq();
        pq.add(u, 0);
        while (!pq.isEmpty()) {
            v = pq.poll();
            if (!visited[v]) {
                visited[v] = true;
                while (i < lenghts[adj[v]]) {
                    to = adj[v];
                    weight = weights[v];
                    if (distances[v] + weight < distances[to]) {
                        distances[to] = distances[v] + weight;
                        previous[to] = v;
                        pq.add(to, distances[to]);
                    }
                    else {}

                    i = i + 1;
                }
            }
            else {}
        }
        return 0;
    }
}