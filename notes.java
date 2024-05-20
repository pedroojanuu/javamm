import PQ;

Dijkstra {

.field public distances.array.i32;
.field public visited.array.i32;
.field public previous.array.i32;

.method dijkstra(u.i32).i32 {
        distances[u.i32].i32 :=.i32 0.i32;
        tmp0.PQ :=.PQ new(PQ).PQ;
        invokespecial(tmp0.PQ,"<init>").V;
        pq.PQ :=.PQ tmp0.PQ;
        invokevirtual(pq.PQ, "add", u.i32, 0.i32).V;
goto while_cond_1;
        while_body_1:
        tmp2.i32 :=.i32 invokevirtual(pq.PQ, "poll").i32;
        v.i32 :=.i32 tmp2.i32;
        tmp3.array.i32 :=.array.i32 getfield(this, visited.array.i32).array.i32;
        tmp4.i32 :=.i32 tmp3.array.i32[v.i32].i32;
        tmp5.bool :=.bool tmp4.i32 <.bool 0.i32;
        tmp6.bool :=.bool !.bool tmp5.bool;
        tmp7.array.i32 :=.array.i32 getfield(this, visited.array.i32).array.i32;
        tmp8.i32 :=.i32 tmp7.array.i32[v.i32].i32;
        tmp9.bool :=.bool 0.i32 <.bool tmp8.i32;
        tmp10.bool :=.bool !.bool tmp9.bool;
        if (tmp6.bool) goto if_then_0;
        tmp11.bool :=.bool 0.bool;
goto if_end_0;
        if_then_0:
        tmp11.bool :=.bool tmp10.bool;
        if_end_0:
        if (tmp11.bool) goto if_then_2;
goto if_end_2;
        if_then_2:
        visited[v.i32].i32 :=.i32 1.i32;
goto while_cond_0;
        while_body_0:
        tmp15.i32 :=.i32 adj.array.i32[v.i32].i32;
        to.i32 :=.i32 tmp15.i32;
        tmp16.i32 :=.i32 weights.array.i32[v.i32].i32;
        weight.i32 :=.i32 tmp16.i32;
        tmp17.array.i32 :=.array.i32 getfield(this, distances.array.i32).array.i32;
        tmp18.i32 :=.i32 tmp17.array.i32[v.i32].i32;
        tmp19.i32 :=.i32 tmp18.i32 +.i32 weight.i32;
        tmp20.array.i32 :=.array.i32 getfield(this, distances.array.i32).array.i32;
        tmp21.i32 :=.i32 tmp20.array.i32[to.i32].i32;
        tmp22.bool :=.bool tmp19.i32 <.bool tmp21.i32;
        if (tmp22.bool) goto if_then_1;
goto if_end_1;
        if_then_1:
        tmp23.array.i32 :=.array.i32 getfield(this, distances.array.i32).array.i32;
        tmp24.i32 :=.i32 tmp23.array.i32[v.i32].i32;
        tmp25.i32 :=.i32 tmp24.i32 +.i32 weight.i32;
        distances[to.i32].i32 :=.i32 tmp25.i32;
        previous[to.i32].i32 :=.i32 v.i32;
        tmp26.array.i32 :=.array.i32 getfield(this, distances.array.i32).array.i32;
        tmp27.i32 :=.i32 tmp26.array.i32[to.i32].i32;
        invokevirtual(pq.PQ, "add", to.i32, tmp27.i32).V;
        if_end_1:
        tmp28.i32 :=.i32 i.i32 +.i32 1.i32;
        i.i32 :=.i32 tmp28.i32;
        while_cond_0:
        tmp12.i32 :=.i32 adj.array.i32[v.i32].i32;
        tmp13.i32 :=.i32 lenghts.array.i32[tmp12.i32].i32;
        tmp14.bool :=.bool i.i32 <.bool tmp13.i32;
        if (tmp14.bool) goto while_body_0;
        if_end_2:
        while_cond_1:
        tmp1.bool :=.bool !.bool invokevirtual(pq.PQ, "isEmpty").V;
        if (tmp1.bool) goto while_body_1;
        ret.i32 0.i32;
    }

.construct Dijkstra().V {
        invokespecial(this, "<init>").V;
    }
}